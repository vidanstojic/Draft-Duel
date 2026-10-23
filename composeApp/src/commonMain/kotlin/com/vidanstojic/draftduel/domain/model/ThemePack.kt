package com.vidanstojic.draftduel.domain.model

data class PackCategory(
    val id: String,
    val name: String,
)

/** All athletes of one theme, with author-assigned ratings (spec §5). */
data class ThemePack(
    val id: String,
    val sport: Sport,
    val name: String,
    val version: Int,
    val categories: List<PackCategory>,
    val cards: List<AthleteCard>,
) {
    fun cardsIn(categoryId: String): List<AthleteCard> = cards.filter { categoryId in it.categoryIds }

    val averageRating: Double get() = cards.map { it.rating }.average()
}
