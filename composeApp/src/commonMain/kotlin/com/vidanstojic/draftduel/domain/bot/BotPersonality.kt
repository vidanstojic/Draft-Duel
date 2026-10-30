package com.vidanstojic.draftduel.domain.bot

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
) {
    /**
     * Goes all in on stars and lowballs everyone else. Flashy and easy to drain in rating packs;
     * in a recipe the main ingredients really are the stars, so there it plays at medium level.
     */
    STAR_CHASER("Galáctico Gio", BotDifficulty.EASY, quickMatchWeight = 15, recipeDifficulty = BotDifficulty.MEDIUM),

    /** Bids on gut feeling, loves forwards, jumps the price for no reason. */
    CASUAL_FAN("Sunday Sam", BotDifficulty.EASY, quickMatchWeight = 20),

    /** Never overpays, waits for steals and spends down over the last cards. */
    BARGAIN_HUNTER("Bargain Bea", BotDifficulty.MEDIUM, quickMatchWeight = 15),

    /** The original spec §12 formula, unchanged. */
    STEADY("Steady Stefan", BotDifficulty.MEDIUM, quickMatchWeight = 25),

    /** Pushes prices past what it wants, just to drain the rival's budget. */
    HUSTLER("Hustler Hugo", BotDifficulty.MEDIUM, quickMatchWeight = 15),

    /** Knows ratings almost exactly and plans the rest of the match with a lookahead. */
    SCOUT("Scout Nina", BotDifficulty.HARD, quickMatchWeight = 10),
    ;

    fun difficultyFor(scoring: Scoring): BotDifficulty = if (scoring == Scoring.RECIPE) recipeDifficulty else ratingsDifficulty

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

    fun create(personality: BotPersonality, stats: PoolStats, random: Random = Random.Default): MatchBot =
        PersonalityBot(behaviorOf(personality, stats), stats, random)

    fun behaviorOf(personality: BotPersonality, stats: PoolStats): BotBehavior = when (personality) {
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
            playsEndgame = false,
        )
        BotPersonality.STAR_CHASER -> BotBehavior(
            perception = RatingPerception(noise = 4, starBonus = 2),
            valuation = StarChaserValuation(),
            style = BidStyle(stepWeights = mapOf(1 to 2, 2 to 1), starOpening = 5),
        )
        BotPersonality.BARGAIN_HUNTER -> BotBehavior(
            perception = RatingPerception(noise = 3),
            valuation = BargainValuation(),
            style = BidStyle(snipes = true),
            thinkMillis = 1_500L..3_500L,
        )
        BotPersonality.HUSTLER -> BotBehavior(
            perception = RatingPerception(noise = 3),
            valuation = SpecFormulaValuation(epsMin = 0.9, epsMax = 1.15),
            style = BidStyle(stepWeights = mapOf(1 to 3, 2 to 1), snipes = true, pushChance = 0.45, pushMargin = 2),
            thinkMillis = 700L..2_000L,
        )
        BotPersonality.SCOUT -> BotBehavior(
            perception = RatingPerception(noise = 2),
            // In rating packs the exact lookahead beat every other bot 9 times in 10, so it
            // plays a little cautious there; recipe matches are close enough without that.
            valuation = if (stats.scoring == Scoring.RATINGS) {
                LookaheadValuation(AuctionLookahead(stats), moodMin = 0.75, moodMax = 1.05)
            } else {
                LookaheadValuation(AuctionLookahead(stats))
            },
            style = BidStyle(snipes = true),
            thinkMillis = 1_500L..4_000L,
        )
    }
}
