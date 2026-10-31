package com.vidanstojic.draftduel.domain.bot

import com.vidanstojic.draftduel.domain.match.MatchAction
import com.vidanstojic.draftduel.domain.match.MatchEngine
import com.vidanstojic.draftduel.domain.match.MatchPhase
import com.vidanstojic.draftduel.domain.match.MatchState
import com.vidanstojic.draftduel.domain.match.Seat
import kotlin.math.min
import kotlin.random.Random

/** How a bot turns "I'd pay up to N" into concrete moves. */
data class BidStyle(
    /** Relative weights of +1 / +2 / +5 when there is room under the limit. Default: always +1. */
    val stepWeights: Map<Int, Int> = mapOf(1 to 1),
    /** Opening bid on cards perceived as stars (rating >= 85), if the limit allows it. */
    val starOpening: Int? = null,
    /** Bids exactly opponentBudget + 1 when that wins outright and is within the limit. */
    val snipes: Boolean = false,
    /** When to pay to hand the opening over (rule sets that allow it); null never does. */
    val handOver: HandOverPolicy? = null,
    /** Chance to raise +1 above its own limit just to make the rival pay more. */
    val pushChance: Double = 0.0,
    /** How far above its limit a push may go. */
    val pushMargin: Int = 0,
)

/** All tunable parts of one personality. */
data class BotBehavior(
    val perception: RatingPerception,
    val valuation: Valuation,
    val style: BidStyle = BidStyle(),
    /** Knows that budget left after the last card is worthless and that bidding past the rival's budget is waste. */
    val playsEndgame: Boolean = true,
    val thinkMillis: LongRange = 1_000L..3_000L,
)

/**
 * A bot assembled from a [BotBehavior]. The guess of each card's rating and the round's mood are
 * fixed the first time the bot looks at them, so it does not change its mind between its own turns.
 */
class PersonalityBot(
    private val behavior: BotBehavior,
    private val stats: PoolStats,
    private val random: Random = Random.Default,
) : MatchBot {

    private val perceived = HashMap<String, Double>()
    private val moods = HashMap<Int, Double>()

    override fun decide(state: MatchState, seat: Seat): MatchAction? = when (val phase = state.phase) {
        is MatchPhase.Bidding -> if (phase.toAct == seat) bid(state, seat, phase) else null
        // No slot limit: a card worth more than 0 always helps its owner, anything else goes to the rival.
        is MatchPhase.Choosing -> if (phase.chooser == seat) choose(state, seat) else null
        else -> null
    }

    override fun thinkTimeMillis(): Long =
        random.nextLong(from = behavior.thinkMillis.first, until = behavior.thinkMillis.last + 1)

    /** The bot's current guess of the card on the table, or null between rounds. */
    fun perceivedRating(state: MatchState): Double? {
        val card = state.currentCard ?: return null
        return perceived.getOrPut(card.id) { behavior.perception.perceive(card, stats, random) }
    }

    /** Highest price this bot will pay for the current card right now, already capped by the rules. */
    fun limit(state: MatchState, seat: Seat): Int {
        val view = view(state, seat) ?: return 0
        val rating = view.perceived
        val budget = view.budget
        val opponentBudget = view.opponentBudget
        // A card worth 0 or less (a wrong ingredient) only lowers its owner's total.
        if (rating <= 0) return 0
        var limit = behavior.valuation.maxPrice(view)
        if (behavior.playsEndgame) {
            if (state.cardsLeft == 0) limit = budget
            limit = min(limit, opponentBudget + 1)
        }
        return limit.coerceIn(0, budget)
    }

    private fun bid(state: MatchState, seat: Seat, phase: MatchPhase.Bidding): MatchAction {
        val limit = limit(state, seat)
        val price = phase.price
        val style = behavior.style

        if (phase.leader == null) {
            val unwanted = (perceivedRating(state) ?: 0.0) <= 0.0
            if (unwanted && MatchEngine.canPass(state, seat)) return MatchAction.Pass(seat)
            if (shouldHandOver(state, seat)) return MatchAction.HandOver(seat)
            return MatchAction.Raise(seat, openingStep(state, seat, limit))
        }

        if (style.snipes) {
            val winningBid = state.seat(seat.other).budget + 1
            val step = winningBid - price
            if (winningBid <= limit && MatchEngine.canRaise(state, seat, step)) return MatchAction.Raise(seat, step)
        }
        if (price + 1 <= limit) return MatchAction.Raise(seat, pickStep(state, seat, room = limit - price))
        if (shouldPush(state, seat, price, limit)) return MatchAction.Raise(seat, 1)
        return MatchAction.Pass(seat)
    }

    private fun view(state: MatchState, seat: Seat): BidView? {
        val rating = perceivedRating(state) ?: return null
        return BidView(
            perceived = rating,
            budget = state.seat(seat).budget,
            opponentBudget = state.seat(seat.other).budget,
            cardsAfter = state.cardsLeft,
            stats = stats,
            mood = moods.getOrPut(state.roundIndex) { random.nextDouble() },
            slotsLeft = state.slotsLeft(seat),
            opponentSlotsLeft = state.slotsLeft(seat.other),
        )
    }

    private fun shouldHandOver(state: MatchState, seat: Seat): Boolean {
        val policy = behavior.style.handOver ?: return false
        if (!MatchEngine.canHandOver(state, seat)) return false
        val view = view(state, seat) ?: return false
        return policy.shouldHandOver(view, MatchEngine.handOverCost(state, seat))
    }

    private fun choose(state: MatchState, seat: Seat): MatchAction {
        val wanted = (perceivedRating(state) ?: 0.0) > 0.0
        return MatchAction.Assign(chooser = seat, to = if (wanted) seat else seat.other)
    }

    /** In rating packs the opener must bid at least 1 even on a card it does not want (spec §3 rule 3). */
    private fun openingStep(state: MatchState, seat: Seat, limit: Int): Int {
        val starOpening = behavior.style.starOpening
        val rating = perceivedRating(state) ?: 0.0
        if (starOpening != null && rating >= stats.starLine && starOpening <= limit &&
            MatchEngine.canRaise(state, seat, starOpening)
        ) {
            return starOpening
        }
        return if (limit >= 1) pickStep(state, seat, room = limit) else 1
    }

    private fun pickStep(state: MatchState, seat: Seat, room: Int): Int {
        val options = behavior.style.stepWeights
            .filter { (step, weight) -> weight > 0 && step <= room && MatchEngine.canRaise(state, seat, step) }
        val total = options.values.sum()
        if (total == 0) return 1
        var roll = random.nextInt(total)
        for ((step, weight) in options) {
            if (roll < weight) return step
            roll -= weight
        }
        return 1
    }

    private fun shouldPush(state: MatchState, seat: Seat, price: Int, limit: Int): Boolean {
        val style = behavior.style
        if (style.pushChance <= 0.0 || state.cardsLeft == 0) return false
        // Pushing a card the rival does not want would just leave it with the pusher.
        if ((perceivedRating(state) ?: 0.0) <= 0.0) return false
        // With a card limit a push that backfires also costs a slot, so only push good cards.
        if (state.ruleSet.slots != null && (view(state, seat)?.strength ?: 0.0) < 1.0) return false
        val next = price + 1
        val affordable = next <= limit + style.pushMargin && MatchEngine.canRaise(state, seat, 1)
        // Pushing past the rival's budget would just buy the card; that is not a push.
        val rivalCanAnswer = next < state.seat(seat.other).budget
        return affordable && rivalCanAnswer && random.nextDouble() < style.pushChance
    }
}
