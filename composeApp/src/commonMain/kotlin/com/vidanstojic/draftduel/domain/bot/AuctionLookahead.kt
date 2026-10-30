package com.vidanstojic.draftduel.domain.bot

import com.vidanstojic.draftduel.domain.match.GameRules
import kotlin.math.max
import kotlin.math.min

/**
 * Small game-tree model of the rest of a match, used by the strongest bot.
 *
 * [value] (b, o, n) is the expected (my total - rival total) from the next n cards when I have b
 * points and the rival has o. The score is a plain sum and past cards cannot change, so the future
 * depends only on budgets and cards left. Assumptions that keep it small:
 * - future ratings are independent draws from [PoolStats.distribution];
 * - both players bid up to their reservation price, so the higher one wins at the other's + 1;
 * - once a player is at 0, the other takes every remaining card for free (spec §3);
 * - when both are at 0 the free cards split evenly (worth 0).
 *
 * Values are memoised; one table serves a whole match (21 x 21 x 10 states at most).
 */
class AuctionLookahead(
    stats: PoolStats,
    private val maxBudget: Int = GameRules.STARTING_BUDGET,
    private val maxCards: Int = GameRules.POOL_SIZE,
) {
    private val ratings: IntArray = stats.distribution.map { it.first }.toIntArray()
    private val probabilities: DoubleArray = stats.distribution.map { it.second }.toDoubleArray()
    private val average: Double = stats.average
    private val memo = DoubleArray((maxBudget + 1) * (maxBudget + 1) * (maxCards + 1)) { Double.NaN }

    /**
     * Highest price worth paying for a card rated [rating] with [cardsAfter] cards still to come:
     * the largest p where winning at p is at least as good as letting the rival win at p.
     * Never above [budget], and never above opponentBudget + 1, which already wins outright.
     */
    fun reservation(rating: Double, budget: Int, opponentBudget: Int, cardsAfter: Int): Int {
        val b = budget.coerceIn(0, maxBudget)
        val o = opponentBudget.coerceIn(0, maxBudget)
        val n = cardsAfter.coerceIn(0, maxCards)
        val cap = min(b, o + 1)
        var best = 0
        for (price in 1..cap) {
            val win = rating + value(b - price, o, n)
            val lose = -rating + value(b, o - min(price, o), n)
            if (win >= lose) best = price
        }
        return best
    }

    fun value(budget: Int, opponentBudget: Int, cards: Int): Double {
        if (cards <= 0) return 0.0
        if (budget == 0 && opponentBudget == 0) return 0.0
        if (opponentBudget == 0) return cards * average
        if (budget == 0) return -cards * average
        val key = (budget * (maxBudget + 1) + opponentBudget) * (maxCards + 1) + cards
        val cached = memo[key]
        if (!cached.isNaN()) return cached

        val after = cards - 1
        var expected = 0.0
        for (i in ratings.indices) {
            val r = ratings[i].toDouble()
            val mine = reservation(r, budget, opponentBudget, after)
            val theirs = reservation(r, opponentBudget, budget, after)
            val outcome = when {
                mine > theirs -> win(r, budget, opponentBudget, after, price = max(1, theirs + 1))
                theirs > mine -> lose(r, budget, opponentBudget, after, price = max(1, mine + 1))
                else -> {
                    val price = max(1, mine)
                    0.5 * (win(r, budget, opponentBudget, after, price) + lose(r, budget, opponentBudget, after, price))
                }
            }
            expected += probabilities[i] * outcome
        }
        memo[key] = expected
        return expected
    }

    private fun win(r: Double, b: Int, o: Int, after: Int, price: Int): Double =
        r + value(b - min(price, b), o, after)

    private fun lose(r: Double, b: Int, o: Int, after: Int, price: Int): Double =
        -r + value(b, o - min(price, o), after)
}
