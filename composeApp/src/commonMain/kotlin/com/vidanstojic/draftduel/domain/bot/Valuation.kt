package com.vidanstojic.draftduel.domain.bot

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** Everything a valuation needs for the card on the table, from the bot's point of view. */
data class BidView(
    /** The bot's guess of the card's rating. */
    val perceived: Double,
    val budget: Int,
    val opponentBudget: Int,
    /** Cards still to be drawn after this one. */
    val cardsAfter: Int,
    val stats: PoolStats,
    /** Uniform [0, 1) sample fixed for this round, for valuations with a mood (spec §12 epsilon). */
    val mood: Double,
    /** Empty slots of the bot and of the rival, or null without a card limit. */
    val slotsLeft: Int? = null,
    val opponentSlotsLeft: Int? = null,
) {
    /**
     * Cards the bot still expects to buy. With a card limit that is its empty slots (spec §12: S);
     * otherwise its half of the remaining cards worth having (wrong ingredients are not counted).
     */
    val fairShare: Int
        get() = slotsLeft?.let { max(1, it) } ?: max(1, ((cardsAfter * stats.usefulShare).roundToInt() + 2) / 2)

    /**
     * (r - floor) / (avg - floor): 1.0 for a typical good card, 0 for one worth nothing or less.
     * With a card limit the floor is the pack's lower quartile instead (a slot has a price).
     */
    val strength: Double
        get() {
            val floor = if (slotsLeft != null) stats.lowerQuartile else stats.floor.toDouble()
            val spread = (stats.average - floor).coerceAtLeast(1.0)
            return ((perceived - floor) / spread).coerceAtLeast(0.0)
        }
}

/** The most a bot is willing to pay for the current card. The bid planner caps it to the rules. */
fun interface Valuation {
    fun maxPrice(view: BidView): Int
}

/**
 * Spec §12: p_max = floor(B / S * (r - 60) / (avg - 60) * eps), with eps in [[epsMin], [epsMax]]
 * and S the bot's fair share of the remaining cards. [scale] makes a cheaper or greedier variant.
 */
class SpecFormulaValuation(
    private val epsMin: Double = 0.8,
    private val epsMax: Double = 1.2,
    private val scale: Double = 1.0,
) : Valuation {
    override fun maxPrice(view: BidView): Int {
        val eps = epsMin + view.mood * (epsMax - epsMin)
        return floor(view.budget.toDouble() / view.fairShare * view.strength * eps * scale).toInt()
    }
}

/** Pays a fraction of the fair price, then spends down evenly over the last few cards. */
class BargainValuation(
    private val discount: Double = 0.85,
    private val endgameCards: Int = 4,
) : Valuation {
    private val fair = SpecFormulaValuation(epsMin = 0.9, epsMax = 1.1)

    override fun maxPrice(view: BidView): Int {
        val cheap = floor(fair.maxPrice(view) * discount).toInt()
        val cardsLeftIncludingThis = view.cardsAfter + 1
        if (cardsLeftIncludingThis > endgameCards) return cheap
        val spendDown = ceil(view.budget.toDouble() / cardsLeftIncludingThis * view.strength.coerceAtMost(1.2)).toInt()
        return max(cheap, spendDown)
    }
}

/** Goes big on athletes it believes are stars and lowballs everyone else until the last cards. */
class StarChaserValuation(
    private val starBudgetShare: Double = 0.4,
    private val othersScale: Double = 0.75,
    private val endgameCards: Int = 2,
) : Valuation {
    private val fair = SpecFormulaValuation()

    override fun maxPrice(view: BidView): Int {
        val normal = fair.maxPrice(view)
        if (view.cardsAfter + 1 <= endgameCards) return normal
        return if (view.perceived >= view.stats.starLine) {
            max(normal, floor(view.budget * starBudgetShare).toInt())
        } else {
            floor(normal * othersScale).toInt()
        }
    }
}

/**
 * Uses [AuctionLookahead]: the price at which winning and losing this card are worth the same,
 * scaled by the round's mood within [[moodMin], [moodMax]] so the bot is strong but not flawless.
 */
class LookaheadValuation(
    private val lookahead: AuctionLookahead,
    private val moodMin: Double = 1.0,
    private val moodMax: Double = 1.0,
) : Valuation {
    override fun maxPrice(view: BidView): Int {
        val reservation = lookahead.reservation(
            rating = view.perceived,
            budget = view.budget,
            opponentBudget = view.opponentBudget,
            cardsAfter = view.cardsAfter,
            mySlots = view.slotsLeft ?: 0,
            theirSlots = view.opponentSlotsLeft ?: 0,
        )
        val mood = moodMin + view.mood * (moodMax - moodMin)
        return (reservation * mood).roundToInt()
    }
}

/**
 * Whether an opener should pay [cost] to let the rival open a card instead (rule sets with
 * hand-over). Only asked when the hand-over is allowed and affordable.
 */
fun interface HandOverPolicy {
    fun shouldHandOver(view: BidView, cost: Int): Boolean
}

/** Hands over cards that look weaker than [below] times a typical good card, while it costs at most [maxCost]. */
class ThresholdHandOver(
    private val below: Double,
    private val maxCost: Int,
) : HandOverPolicy {
    override fun shouldHandOver(view: BidView, cost: Int): Boolean = cost <= maxCost && view.strength < below
}

/** Compares opening and handing over with [AuctionLookahead]. */
class LookaheadHandOver(private val lookahead: AuctionLookahead) : HandOverPolicy {
    override fun shouldHandOver(view: BidView, cost: Int): Boolean = lookahead.handOverGain(
        rating = view.perceived,
        budget = view.budget,
        opponentBudget = view.opponentBudget,
        cardsAfter = view.cardsAfter,
        mySlots = view.slotsLeft ?: 0,
        theirSlots = view.opponentSlotsLeft ?: 0,
        cost = cost,
    ) > 0.0
}
