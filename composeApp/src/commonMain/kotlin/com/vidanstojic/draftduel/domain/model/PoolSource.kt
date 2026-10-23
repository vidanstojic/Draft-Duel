package com.vidanstojic.draftduel.domain.model

/** Where the 10 cards of a match come from (spec §5). */
sealed interface PoolSource {
    data object Random : PoolSource

    data class ByCategory(val categoryId: String) : PoolSource

    /** Friend matches only: the creator hand-picks the athletes. */
    data object Custom : PoolSource
}
