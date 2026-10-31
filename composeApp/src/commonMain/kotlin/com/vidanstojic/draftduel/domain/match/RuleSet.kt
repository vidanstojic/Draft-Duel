package com.vidanstojic.draftduel.domain.match

/** Rule variants a match can be played with. Chosen before the match, same for both players. */
enum class RuleSet(
    /** Cards a player can hold; null means no limit. */
    val slots: Int?,
    /** The opener may pay to hand the opening of a card over to the rival. */
    val allowsHandOver: Boolean,
) {
    /** No card limit; only the budget limits a player, and the opener must bid. */
    CLASSIC(slots = null, allowsHandOver = false),

    /**
     * Five slots each (spec §3 v1): once a player is full, the remaining cards go to the other
     * for free. The opener may hand the opening over instead of bidding on a card they do not
     * want: once per card, the rival must then open. Each hand-over costs that player one point
     * more than the last (1, 2, 3…) and the points are simply gone.
     */
    FIVE_SLOTS(slots = 5, allowsHandOver = true),
}
