package com.vidanstojic.draftduel.domain.model

/** The cards drawn for one match. */
data class MatchPool(
    /** Shuffled, in the order they will be auctioned. */
    val cards: List<GameCard>,
    /** What the pool was drawn from, i.e. what a player (or bot) may know before the match. */
    val drawnFrom: List<GameCard>,
    /** The dish to build in recipe packs; null in rating packs. */
    val goal: PackCategory? = null,
)
