package com.vidanstojic.draftduel.domain.match

import com.vidanstojic.draftduel.domain.model.Theme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/** What the numbers of a [FinalScore] count. */
enum class ScoreUnit {
    /** Sum of card ratings (movies), or recipe points (food): the internal totals as they are. */
    TOTALS,

    /** Football: a scoreline like 2:1. */
    GOALS,

    /** Basketball: a scoreline like 104:97. */
    POINTS,
}

/** The result as shown at the end: [a] and [b] are seats A and B. */
data class FinalScore(
    val a: Int,
    val b: Int,
    val unit: ScoreUnit,
) {
    fun of(seat: Seat): Int = if (seat == Seat.A) a else b

    val margin: Int get() = abs(a - b)
}

/**
 * Turns the rating totals into a score that looks like a real match of the sport.
 *
 * The winner is always decided by the totals ([MatchEngine.outcome]); this only says how the win
 * reads. The size of the win comes from the rating gap per card, so it does not depend on how many
 * cards a match has: a few points better per card is a one-goal or a tight basketball win, a team
 * a whole tier better wins 4:0 or by 25. How many the loser scores comes from the loser's own
 * strength, plus a little chance, so two matches with the same gap do not end the same way.
 * The chance is seeded by the match itself, so the same match always shows the same score.
 */
object ScoreProjection {

    fun of(state: MatchState): FinalScore {
        val a = state.seat(Seat.A)
        val b = state.seat(Seat.B)
        val unit = unitFor(state.theme)
        if (unit == ScoreUnit.TOTALS) return FinalScore(a.total, b.total, unit)
        val random = Random(seedOf(state))
        val cardsPerSide = ((a.cards.size + b.cards.size) / 2.0).coerceAtLeast(1.0)
        val gap = (a.total - b.total) / cardsPerSide
        val loser = if (gap >= 0) b else a
        val loserAverage = if (loser.cards.isEmpty()) 0.0 else loser.total.toDouble() / loser.cards.size
        val (winnerScore, loserScore) = when (unit) {
            ScoreUnit.GOALS -> football(abs(gap), random)
            else -> basketball(abs(gap), loserAverage, random)
        }
        return when {
            a.total == b.total -> FinalScore(loserScore, loserScore, unit)
            gap > 0 -> FinalScore(winnerScore, loserScore, unit)
            else -> FinalScore(loserScore, winnerScore, unit)
        }
    }

    fun unitFor(theme: Theme): ScoreUnit = when (theme) {
        Theme.FOOTBALL -> ScoreUnit.GOALS
        Theme.BASKETBALL -> ScoreUnit.POINTS
        Theme.TENNIS, Theme.MOVIES, Theme.FOOD -> ScoreUnit.TOTALS
    }

    /** Goals (winner, loser). [gap] is the rating gap per card; 0 means a draw. */
    internal fun football(gap: Double, random: Random): Pair<Int, Int> {
        if (gap == 0.0) return pick(DRAW_GOALS, random).let { it to it }
        val margin = FOOTBALL_MARGINS.indexOfFirst { gap < it }.let { if (it == -1) FOOTBALL_MARGINS.size + 1 else it + 1 }
        val loser = pick(LOSER_GOALS[(margin - 1).coerceAtMost(LOSER_GOALS.lastIndex)], random)
        return (loser + margin) to loser
    }

    /** Points (winner, loser). The loser's points follow their average card; the gap sets the margin. */
    internal fun basketball(gap: Double, loserAverage: Double, random: Random): Pair<Int, Int> {
        if (gap == 0.0) {
            val tied = (BASE_POINTS + random.nextInt(-6, 7)).coerceIn(MIN_POINTS, MAX_POINTS)
            return tied to tied
        }
        val margin = (1 + POINTS_PER_RATING * gap + random.nextInt(-2, 3)).roundToInt().coerceIn(1, MAX_MARGIN)
        val quality = if (loserAverage > 0) (loserAverage - AVERAGE_CARD) * POINTS_PER_QUALITY else 0.0
        val loser = (BASE_POINTS + quality + random.nextInt(-7, 8)).roundToInt().coerceIn(MIN_POINTS, MAX_POINTS)
        return (loser + margin) to loser
    }

    private fun pick(weights: IntArray, random: Random): Int {
        var roll = random.nextInt(weights.sum())
        weights.forEachIndexed { goals, weight ->
            if (roll < weight) return goals
            roll -= weight
        }
        return 0
    }

    /** Same match, same score: the cards, who got them and for how much. */
    private fun seedOf(state: MatchState): Int = Seat.entries.fold(state.pool.size) { acc, seat ->
        state.seat(seat).cards.fold(acc * 31 + seat.ordinal) { h, owned -> h * 31 + owned.card.id.hashCode() * 7 + owned.price }
    }

    /** Upper bounds of the rating gap per card for a 1, 2, 3 and 4 goal win; above the last, 5. */
    private val FOOTBALL_MARGINS = doubleArrayOf(3.0, 6.5, 10.5, 15.0)

    /** Chance (weights) of the loser scoring 0, 1, 2, 3 goals, by the winning margin 1, 2, 3, 4+. */
    private val LOSER_GOALS = arrayOf(
        intArrayOf(35, 40, 20, 5),
        intArrayOf(45, 40, 15),
        intArrayOf(55, 35, 10),
        intArrayOf(70, 30),
    )
    private val DRAW_GOALS = intArrayOf(30, 40, 25, 5)

    private const val BASE_POINTS = 92
    private const val AVERAGE_CARD = 78.0
    private const val POINTS_PER_QUALITY = 1.0
    private const val POINTS_PER_RATING = 3.0
    private const val MIN_POINTS = 68
    private const val MAX_POINTS = 128
    private const val MAX_MARGIN = 42
}
