package com.vidanstojic.draftduel.domain.bot

/**
 * What a bot may know about the deck before the match: the ratings of the cards the pool is
 * drawn from (the whole pack, or one category), but not which 10 were drawn or in what order.
 */
class PoolStats(ratings: List<Int>) {

    init {
        require(ratings.isNotEmpty()) { "PoolStats needs at least one rating." }
    }

    val average: Double = ratings.average()

    /** Distinct ratings and their probability of being the next card (draws treated as independent). */
    val distribution: List<Pair<Int, Double>> = ratings
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedBy { it.key }
        .map { (rating, count) -> rating to count.toDouble() / ratings.size }
}
