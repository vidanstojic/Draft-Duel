package com.vidanstojic.draftduel.domain.model

import com.vidanstojic.draftduel.domain.match.GameRules

data class PackCategory(
    val id: String,
    val name: String,
)

/** All athletes of one theme, with author-assigned ratings (spec §5). */
data class ThemePack(
    val id: String,
    val theme: Theme,
    val name: String,
    val version: Int,
    val categories: List<PackCategory>,
    val cards: List<AthleteCard>,
) {
    fun cardsIn(categoryId: String): List<AthleteCard> = cards.filter { categoryId in it.categoryIds }

    val averageRating: Double get() = cards.map { it.rating }.average()

    /**
     * The cards a pool for [source] is drawn from: the category when it has at least [poolSize]
     * athletes (spec §5), otherwise the whole pack.
     */
    fun candidatesFor(source: PoolSource, poolSize: Int = GameRules.POOL_SIZE): List<AthleteCard> = when (source) {
        is PoolSource.ByCategory -> cardsIn(source.categoryId).takeIf { it.size >= poolSize } ?: cards
        PoolSource.Random, PoolSource.Custom -> cards
    }
}
