package com.vidanstojic.draftduel.domain.model

/**
 * One auction card: an athlete in one season.
 *
 * The rating is hidden during bidding. The owner sees it once the card is won and the opponent
 * only at the final reveal.
 */
data class AthleteCard(
    val id: String,
    val name: String,
    /** Season label, e.g. "2011/12". */
    val season: String,
    /** Country of the league the athlete played in that season (no club names or crests). */
    val country: String,
    /** Short position code, e.g. "FWD" or "PG". */
    val position: String,
    /** 60–99, assigned by the pack author. */
    val rating: Int,
    val avatar: AvatarSpec,
    val categoryIds: Set<String>,
)
