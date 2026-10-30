package com.vidanstojic.draftduel.domain.bot

import com.vidanstojic.draftduel.domain.model.GameCard
import com.vidanstojic.draftduel.domain.model.Scoring
import kotlin.math.abs

/**
 * What a bot may know before the match: the values of the cards the pool is drawn from (the
 * pack, a category, or a dish's ingredient list), but not which ones were drawn or their order.
 */
class PoolStats(
    values: List<Int>,
    val scoring: Scoring = Scoring.RATINGS,
) {
    init {
        require(values.isNotEmpty()) { "PoolStats needs at least one value." }
    }

    /** A card worth exactly this adds nothing worth paying for (rating 60, or 0 points). */
    val floor: Int = scoring.floor

    val min: Int = values.min()
    val max: Int = values.max()

    /** Average of the cards worth having, the "typical good card" valuations compare against. */
    val average: Double = values.filter { it > floor }.ifEmpty { values }.average()

    /** Expected |value|: what a free pick is worth when the rival is on 0 (good cards kept, bad ones given away). */
    val averageMagnitude: Double = values.map { abs(it) }.average()

    /** Cards at or above this look like stars (about 85 for 60–99 ratings, about 18 points in a recipe). */
    val starLine: Double = floor + (max - floor) * STAR_SHARE

    /** Recipe packs let the opener skip a card nobody wants. */
    val openerMaySkip: Boolean get() = scoring == Scoring.RECIPE

    /** Distinct values and their probability of being the next card (draws treated as independent). */
    val distribution: List<Pair<Int, Double>> = values
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedBy { it.key }
        .map { (value, count) -> value to count.toDouble() / values.size }

    companion object {
        private const val STAR_SHARE = 0.6

        fun of(cards: List<GameCard>, scoring: Scoring): PoolStats = PoolStats(cards.map { it.rating }, scoring)
    }
}
