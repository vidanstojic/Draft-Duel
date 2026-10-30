package com.vidanstojic.draftduel.domain.bot

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

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
) {
    /** Cards the bot expects to buy from here on if both sides split them evenly. */
    val fairShare: Int get() = max(1, (cardsAfter + 2) / 2)

    /** (r - floor) / (avg - floor): 1.0 for a typical good card, 0 for one worth nothing or less. */
    val strength: Double
        get() {
            val spread = (stats.average - stats.floor).coerceAtLeast(1.0)
            return ((perceived - stats.floor) / spread).coerceAtLeast(0.0)
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

/** Uses [AuctionLookahead]: the price at which winning and losing this card are worth the same. */
class LookaheadValuation(private val lookahead: AuctionLookahead) : Valuation {
    override fun maxPrice(view: BidView): Int =
        lookahead.reservation(view.perceived, view.budget, view.opponentBudget, view.cardsAfter)
}
