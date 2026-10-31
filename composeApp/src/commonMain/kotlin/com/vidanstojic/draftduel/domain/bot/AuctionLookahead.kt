package com.vidanstojic.draftduel.domain.bot

import com.vidanstojic.draftduel.domain.match.GameRules
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Small game-tree model of the rest of a match, used by the strongest bot.
 *
 * [value] (b, o, n, mySlots, theirSlots) is the expected (my total - rival total) from the next n
 * cards when I have b points and the rival has o. The score is a plain sum and past cards cannot
 * change, so the future depends only on budgets, cards left and, with a card limit, empty slots.
 * Assumptions that keep it small:
 * - future values are independent draws from [PoolStats.distribution];
 * - both players bid up to their reservation price, so the higher one wins at the other's + 1;
 * - once a player is at 0, the other picks for free: keeps good cards, hands over bad ones (spec §3);
 *   with [slots] the card counts are fixed, so the picker's edge is keeping the best of the rest
 *   (expected order statistics of the remaining draws);
 * - with [slots], a full player gets nothing more and the rest go to the other for free;
 * - in recipe packs a card nobody wants is skipped by the opener;
 * - when both are at 0 the free cards split evenly (worth 0).
 *
 * Values are memoised; one table serves a whole match.
 */
class AuctionLookahead(
    stats: PoolStats,
    /** Cards a player can hold, or null for no limit. */
    private val slots: Int? = null,
    private val maxBudget: Int = GameRules.STARTING_BUDGET,
    private val maxCards: Int = GameRules.POOL_SIZE,
) {
    private val ratings: IntArray = stats.distribution.map { it.first }.toIntArray()
    private val probabilities: DoubleArray = stats.distribution.map { it.second }.toDoubleArray()
    private val average: Double = stats.average
    private val freePickValue: Double = stats.averageMagnitude
    private val openerMaySkip: Boolean = stats.openerMaySkip
    private val slotStates = (slots ?: 0) + 1
    /** topSums[n][k]: expected sum of the best k of n remaining cards. */
    private val topSums: Array<DoubleArray> = Array(maxCards + 1) { n -> DoubleArray(n + 1) { k -> expectedTopSum(n, k) } }
    private val tableSize = (maxBudget + 1) * (maxBudget + 1) * (maxCards + 1) * slotStates * slotStates

    /** A flat array for the tested rules; custom rules with big budgets and limits only fill a few states, so a map. */
    private val memoArray: DoubleArray? = if (tableSize <= ARRAY_MEMO_LIMIT) DoubleArray(tableSize) { Double.NaN } else null
    private val memoMap: MutableMap<Int, Double>? = if (memoArray == null) HashMap() else null

    /**
     * Highest price worth paying for a card rated [rating] with [cardsAfter] cards still to come:
     * the largest p where winning at p is at least as good as letting the rival win at p.
     * Never above [budget], and never above opponentBudget + 1, which already wins outright.
     * Slot counts are ignored without a card limit.
     */
    fun reservation(
        rating: Double,
        budget: Int,
        opponentBudget: Int,
        cardsAfter: Int,
        mySlots: Int = 0,
        theirSlots: Int = 0,
    ): Int {
        val b = budget.coerceIn(0, maxBudget)
        val o = opponentBudget.coerceIn(0, maxBudget)
        val n = cardsAfter.coerceIn(0, maxCards)
        val cap = min(b, o + 1)
        var best = 0
        for (price in 1..cap) {
            if (win(rating, b, o, n, mySlots, theirSlots, price) >= lose(rating, b, o, n, mySlots, theirSlots, price)) best = price
        }
        return best
    }

    /**
     * How much better handing the opening over (and paying [cost]) is than opening this card
     * yourself; positive means hand it over. Both sides are modelled as bidding up to their
     * reservation price, and whoever opens a card nobody wants gets it for 1.
     */
    fun handOverGain(
        rating: Double,
        budget: Int,
        opponentBudget: Int,
        cardsAfter: Int,
        mySlots: Int,
        theirSlots: Int,
        cost: Int,
    ): Double {
        if (cost > budget) return Double.NEGATIVE_INFINITY
        val open = openerOutcome(rating, budget, opponentBudget, cardsAfter, mySlots, theirSlots, iOpen = true)
        val handed = openerOutcome(rating, budget - cost, opponentBudget, cardsAfter, mySlots, theirSlots, iOpen = false)
        return handed - open
    }

    fun value(budget: Int, opponentBudget: Int, cards: Int, mySlots: Int = 0, theirSlots: Int = 0): Double {
        if (cards <= 0) return 0.0
        if (slots != null) {
            if (mySlots == 0 && theirSlots == 0) return 0.0
            if (mySlots == 0) return -min(cards, theirSlots) * average
            if (theirSlots == 0) return min(cards, mySlots) * average
        }
        if (budget == 0 && opponentBudget == 0) return 0.0
        if (opponentBudget == 0) return freePicks(cards, mySlots)
        if (budget == 0) return -freePicks(cards, theirSlots)
        val key = key(budget, opponentBudget, cards, mySlots, theirSlots)
        val cached = memoArray?.get(key) ?: memoMap?.get(key) ?: Double.NaN
        if (!cached.isNaN()) return cached

        val after = cards - 1
        var expected = 0.0
        for (i in ratings.indices) {
            val r = ratings[i].toDouble()
            val mine = reservation(r, budget, opponentBudget, after, mySlots, theirSlots)
            val theirs = reservation(r, opponentBudget, budget, after, theirSlots, mySlots)
            val outcome = when {
                mine > theirs -> win(r, budget, opponentBudget, after, mySlots, theirSlots, price = max(1, theirs + 1))
                theirs > mine -> lose(r, budget, opponentBudget, after, mySlots, theirSlots, price = max(1, mine + 1))
                // Nobody wants it: in recipe packs the opener skips it and it is gone.
                mine == 0 && r <= 0 && openerMaySkip -> value(budget, opponentBudget, after, mySlots, theirSlots)
                else -> {
                    val price = max(1, mine)
                    0.5 * (win(r, budget, opponentBudget, after, mySlots, theirSlots, price) +
                        lose(r, budget, opponentBudget, after, mySlots, theirSlots, price))
                }
            }
            expected += probabilities[i] * outcome
        }
        if (memoArray != null) memoArray[key] = expected else memoMap?.set(key, expected)
        return expected
    }

    /** The current card when one side opens: a card nobody wants goes to the opener for 1. */
    private fun openerOutcome(r: Double, b: Int, o: Int, after: Int, sa: Int, sb: Int, iOpen: Boolean): Double {
        if (b == 0 && o == 0) return 0.0
        val mine = reservation(r, b, o, after, sa, sb)
        val theirs = reservation(r, o, b, after, sb, sa)
        return when {
            mine > theirs -> win(r, b, o, after, sa, sb, price = max(1, theirs + 1))
            theirs > mine -> lose(r, b, o, after, sa, sb, price = max(1, mine + 1))
            mine == 0 -> if (iOpen && b > 0) win(r, b, o, after, sa, sb, price = 1) else lose(r, b, o, after, sa, sb, price = 1)
            else -> 0.5 * (win(r, b, o, after, sa, sb, mine) + lose(r, b, o, after, sa, sb, mine))
        }
    }

    /**
     * Rival on 0: without a limit every card is picked freely. With one, the picker fills their
     * slots with the best of the remaining cards and the rest go to the rival.
     */
    private fun freePicks(cards: Int, pickerSlots: Int): Double {
        if (slots == null) return cards * freePickValue
        val n = cards.coerceIn(0, maxCards)
        val kept = min(n, pickerSlots)
        val best = topSums[n][kept]
        val all = topSums[n][n]
        return best - (all - best)
    }

    /**
     * E[sum of the k largest of n independent draws] from the distribution, via order statistics:
     * P(X(i) <= v) = sum over j >= i of C(n, j) F(v)^j (1 - F(v))^(n - j), with X(1) the smallest.
     */
    private fun expectedTopSum(n: Int, k: Int): Double {
        if (n == 0 || k == 0) return 0.0
        var total = 0.0
        for (i in (n - k + 1)..n) {
            var previousCdf = 0.0
            var cumulative = 0.0
            for (index in ratings.indices) {
                cumulative += probabilities[index]
                val cdf = orderStatisticCdf(i, n, cumulative.coerceAtMost(1.0))
                total += ratings[index] * (cdf - previousCdf)
                previousCdf = cdf
            }
        }
        return total
    }

    private fun orderStatisticCdf(i: Int, n: Int, f: Double): Double {
        var sum = 0.0
        for (j in i..n) sum += binomial(n, j) * f.pow(j) * (1 - f).pow(n - j)
        return sum
    }

    private fun binomial(n: Int, k: Int): Double {
        var result = 1.0
        for (step in 1..k) result = result * (n - k + step) / step
        return result
    }

    private fun win(r: Double, b: Int, o: Int, after: Int, sa: Int, sb: Int, price: Int): Double =
        r + value(b - min(price, b), o, after, slotsAfterTaking(sa), sb)

    private fun lose(r: Double, b: Int, o: Int, after: Int, sa: Int, sb: Int, price: Int): Double =
        -r + value(b, o - min(price, o), after, sa, slotsAfterTaking(sb))

    private fun slotsAfterTaking(slotsLeft: Int): Int = if (slots == null) 0 else (slotsLeft - 1).coerceAtLeast(0)

    private fun key(b: Int, o: Int, n: Int, sa: Int, sb: Int): Int {
        val mySlots = if (slots == null) 0 else sa.coerceIn(0, slots)
        val theirSlots = if (slots == null) 0 else sb.coerceIn(0, slots)
        return (((b * (maxBudget + 1) + o) * (maxCards + 1) + n) * slotStates + mySlots) * slotStates + theirSlots
    }

    private companion object {
        /** 20 points, 10 cards and 5 slots need 21 x 21 x 11 x 6 x 6 = 174 636 states. */
        const val ARRAY_MEMO_LIMIT = 200_000
    }
}
