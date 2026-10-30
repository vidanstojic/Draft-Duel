package com.vidanstojic.draftduel.domain.model

/** Theme of a pack. It decides how cards score and which surface is drawn behind a match. */
enum class Theme(val scoring: Scoring) {
    FOOTBALL(Scoring.RATINGS),
    BASKETBALL(Scoring.RATINGS),
    TENNIS(Scoring.RATINGS),
    MOVIES(Scoring.RATINGS),
    FOOD(Scoring.RECIPE),
}

enum class Scoring {
    /** Every card is rated 60–99 and the higher total wins (spec §4). */
    RATINGS,

    /**
     * A match is about one dish (a category). Its ingredients add points that sum to 100, wrong
     * ingredients take points away, so the total says how close a player got to the recipe.
     * The opener may skip a card without bidding; nobody gets it.
     */
    RECIPE,
    ;

    /** The value of a card that is worth nothing: the rating floor, or 0 points. */
    val floor: Int get() = if (this == RATINGS) 60 else 0
}
