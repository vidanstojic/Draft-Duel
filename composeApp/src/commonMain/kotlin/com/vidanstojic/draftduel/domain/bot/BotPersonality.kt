package com.vidanstojic.draftduel.domain.bot

import com.vidanstojic.draftduel.domain.match.MatchRules
import com.vidanstojic.draftduel.domain.model.Scoring
import kotlin.random.Random

/**
 * The bots a player can face, ordered from easiest to hardest. [nickname] is a proper name shown in
 * the match, so it is not translated; taglines live in string resources.
 *
 * Difficulty comes from bot-vs-bot simulation (`BotSimulationTest`, average win rate against the
 * other five on each pack); see the README for the latest table.
 */
enum class BotPersonality(
    val nickname: String,
    /** Difficulty in rating packs (football, movies). */
    private val ratingsDifficulty: BotDifficulty,
    /** Relative chance of meeting this bot behind "Find opponent". */
    val quickMatchWeight: Int,
    /** Difficulty in recipe packs (food), where some styles play differently. */
    private val recipeDifficulty: BotDifficulty = ratingsDifficulty,
    /** Difficulty with a card limit ([MatchRules.slots], five by default), where stars matter again. */
    private val fiveSlotsDifficulty: BotDifficulty = ratingsDifficulty,
) {
    /**
     * Goes all in on stars and lowballs everyone else. Flashy and easy to drain in rating packs;
     * in a recipe the main ingredients really are the stars, so there it plays at medium level.
     */
    STAR_CHASER(
        "Galáctico Gio",
        BotDifficulty.EASY,
        quickMatchWeight = 15,
        recipeDifficulty = BotDifficulty.MEDIUM,
        fiveSlotsDifficulty = BotDifficulty.MEDIUM,
    ),

    /** Bids on gut feeling, loves forwards, jumps the price for no reason. */
    CASUAL_FAN("Sunday Sam", BotDifficulty.EASY, quickMatchWeight = 20),

    /** Never overpays, waits for steals and spends down over the last cards. */
    BARGAIN_HUNTER("Bargain Bea", BotDifficulty.MEDIUM, quickMatchWeight = 15),

    /** The original spec §12 formula, unchanged. */
    STEADY("Steady Stefan", BotDifficulty.MEDIUM, quickMatchWeight = 25),

    /**
     * Pushes prices past what it wants, just to drain the rival's budget. With five slots a push
     * that backfires also costs a slot, which makes it an easy opponent there.
     */
    HUSTLER("Hustler Hugo", BotDifficulty.MEDIUM, quickMatchWeight = 15, fiveSlotsDifficulty = BotDifficulty.EASY),

    /** Knows ratings almost exactly and plans the rest of the match with a lookahead. */
    SCOUT("Scout Nina", BotDifficulty.HARD, quickMatchWeight = 10),
    ;

    fun difficultyFor(scoring: Scoring, rules: MatchRules = MatchRules.CLASSIC): BotDifficulty = when {
        rules.slots != null && scoring == Scoring.RATINGS -> fiveSlotsDifficulty
        scoring == Scoring.RECIPE -> recipeDifficulty
        else -> ratingsDifficulty
    }

    companion object {
        /** Hidden opponent for "Find opponent" and the invite fallback. */
        fun randomForQuickMatch(random: Random = Random.Default): BotPersonality {
            var roll = random.nextInt(entries.sumOf { it.quickMatchWeight })
            for (personality in entries) {
                if (roll < personality.quickMatchWeight) return personality
                roll -= personality.quickMatchWeight
            }
            return entries.first()
        }
    }
}

enum class BotDifficulty { EASY, MEDIUM, HARD }

/** Builds the bot for one match. Parameters are tuned with `BotSimulationTest` on the football pack. */
object BotFactory {

    private data class LookaheadKey(
        val distribution: List<Pair<Int, Double>>,
        val scoring: Scoring,
        val slots: Int?,
        val budget: Int,
        val cards: Int,
    )

    private const val LOOKAHEAD_CACHE_SIZE = 4

    /**
     * The lookahead table depends only on the pool's value distribution and the rule set, which
     * repeat from match to match (the same pack, a rematch), so a few recent tables are kept.
     */
    private val lookaheads = LinkedHashMap<LookaheadKey, AuctionLookahead>()

    private fun lookaheadFor(stats: PoolStats, rules: MatchRules): AuctionLookahead {
        val key = LookaheadKey(stats.distribution, stats.scoring, rules.slots, rules.budget, rules.cards)
        lookaheads.remove(key)?.let { cached ->
            lookaheads[key] = cached
            return cached
        }
        val created = AuctionLookahead(stats, slots = rules.slots, maxBudget = rules.budget, maxCards = rules.cards)
        lookaheads[key] = created
        if (lookaheads.size > LOOKAHEAD_CACHE_SIZE) lookaheads.remove(lookaheads.keys.first())
        return created
    }

    fun create(
        personality: BotPersonality,
        stats: PoolStats,
        random: Random = Random.Default,
        rules: MatchRules = MatchRules.CLASSIC,
    ): MatchBot = PersonalityBot(behaviorOf(personality, stats, rules), stats, random)

    /**
     * Hand-over policies only matter when the rules allow it ([MatchRules.handOver]): the casual
     * fan never thinks of it and the hustler would rather push the price than pay to dodge.
     */
    fun behaviorOf(personality: BotPersonality, stats: PoolStats, rules: MatchRules = MatchRules.CLASSIC): BotBehavior = when (personality) {
        BotPersonality.CASUAL_FAN -> BotBehavior(
            perception = RatingPerception(
                noise = 9,
                positionBias = mapOf("FWD" to 5, "MID" to 1, "DEF" to -3, "GK" to -5),
                starBonus = 3,
            ),
            valuation = SpecFormulaValuation(epsMin = 0.6, epsMax = 1.6),
            style = BidStyle(stepWeights = mapOf(1 to 6, 2 to 3, 5 to 1)),
            playsEndgame = false,
            thinkMillis = 800L..2_500L,
        )
        BotPersonality.STEADY -> BotBehavior(
            perception = RatingPerception(noise = 6),
            valuation = SpecFormulaValuation(),
            style = BidStyle(handOver = ThresholdHandOver(below = 0.5, maxCost = 1)),
            playsEndgame = false,
        )
        BotPersonality.STAR_CHASER -> BotBehavior(
            perception = RatingPerception(noise = 4, starBonus = 2),
            valuation = StarChaserValuation(),
            style = BidStyle(
                stepWeights = mapOf(1 to 2, 2 to 1),
                starOpening = 5,
                // Filler is not worth a slot to a star chaser, so it pays to dodge it.
                handOver = ThresholdHandOver(below = 1.0, maxCost = 2),
            ),
        )
        BotPersonality.BARGAIN_HUNTER -> BotBehavior(
            perception = RatingPerception(noise = 3),
            valuation = BargainValuation(),
            style = BidStyle(snipes = true, handOver = ThresholdHandOver(below = 0.7, maxCost = 2)),
            thinkMillis = 1_500L..3_500L,
        )
        BotPersonality.HUSTLER -> BotBehavior(
            perception = RatingPerception(noise = 3),
            valuation = SpecFormulaValuation(epsMin = 0.9, epsMax = 1.15),
            style = BidStyle(stepWeights = mapOf(1 to 3, 2 to 1), snipes = true, pushChance = 0.45, pushMargin = 2),
            thinkMillis = 700L..2_000L,
        )
        BotPersonality.SCOUT -> {
            val lookahead = lookaheadFor(stats, rules)
            BotBehavior(
                perception = RatingPerception(noise = 2),
                // In classic rating matches the exact lookahead beat every other bot 9 times in 10,
                // so it plays a little cautious there; recipes and five slots are close enough without that.
                valuation = if (stats.scoring == Scoring.RATINGS && rules.slots == null) {
                    LookaheadValuation(lookahead, moodMin = 0.75, moodMax = 1.05)
                } else {
                    LookaheadValuation(lookahead)
                },
                style = BidStyle(snipes = true, handOver = LookaheadHandOver(lookahead)),
                thinkMillis = 1_500L..4_000L,
            )
        }
    }
}
