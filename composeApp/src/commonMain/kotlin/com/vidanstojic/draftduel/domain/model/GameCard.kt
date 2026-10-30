package com.vidanstojic.draftduel.domain.model

/**
 * One auction card: an athlete in one season, a movie, or an ingredient.
 *
 * The [rating] is hidden during bidding. The owner sees it once the card is won and the opponent
 * only at the final reveal.
 */
data class GameCard(
    val id: String,
    val name: String,
    /** Season label ("2011/12"), release year for movies, empty for ingredients. */
    val season: String,
    /** League country for athletes, production country for movies, empty for ingredients. */
    val country: String,
    /** Short tag in the card corner: position ("FWD", "PG"), genre ("SCIFI") or food group ("DAIRY"). */
    val position: String,
    /**
     * Points the card adds to its owner's total. 60–99 in rating packs; in recipe packs the
     * ingredient's share of the dish, negative for ingredients that do not belong.
     */
    val rating: Int,
    val avatar: AvatarSpec,
    val categoryIds: Set<String>,
) {
    /** Season, country and tag joined for one-line layouts, skipping empty parts. */
    val details: String get() = listOf(season, country.uppercase(), position).filter { it.isNotBlank() }.joinToString(" · ")
}
