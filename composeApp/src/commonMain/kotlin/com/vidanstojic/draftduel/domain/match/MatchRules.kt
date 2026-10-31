package com.vidanstojic.draftduel.domain.match

/** Price of handing a card over: [firstCost] for a player's first, then [increment] more each time. */
data class HandOverRule(
    val firstCost: Int = 1,
    val increment: Int = 1,
) {
    init {
        require(firstCost in FIRST_COST_RANGE) { "firstCost $firstCost outside $FIRST_COST_RANGE" }
        require(increment in INCREMENT_RANGE) { "increment $increment outside $INCREMENT_RANGE" }
    }

    /** Cost of the next hand-over for a player who has already used [used]. */
    fun cost(used: Int): Int = firstCost + used * increment

    companion object {
        val FIRST_COST_RANGE = 1..3
        val INCREMENT_RANGE = 0..2
    }
}

/**
 * Everything that can be tuned about a match. Chosen before the match, the same for both players.
 * [DEFAULT] is the rule set the game is tested with; [CUSTOM] values exist for play-testing and
 * bots are tuned for [DEFAULT] and [CLASSIC] only.
 */
data class MatchRules(
    /** Points each player starts with. */
    val budget: Int = GameRules.STARTING_BUDGET,
    /** Cards auctioned in one match. */
    val cards: Int = GameRules.POOL_SIZE,
    /**
     * Cards a player can hold, or null for no limit. Once a player is full the remaining cards go
     * to the other for free; when both are full the match ends.
     */
    val slots: Int? = 5,
    /**
     * Before the first bid the opener may pay to make the rival open the card instead; the rival
     * cannot hand it back. Null: the opener must bid (spec §3 rule 3).
     */
    val handOver: HandOverRule? = HandOverRule(),
    val turnSeconds: Int = GameRules.TURN_SECONDS,
    /** Spec §4 public ratings: both players see every rating while bidding. Off: ratings stay hidden. */
    val ratingsVisible: Boolean = false,
) {
    init {
        require(budget in BUDGET_RANGE) { "budget $budget outside $BUDGET_RANGE" }
        require(cards in CARDS_RANGE) { "cards $cards outside $CARDS_RANGE" }
        require(slots == null || slots in SLOTS_RANGE) { "slots $slots outside $SLOTS_RANGE" }
        require(turnSeconds in TURN_SECONDS_RANGE) { "turnSeconds $turnSeconds outside $TURN_SECONDS_RANGE" }
    }

    val allowsHandOver: Boolean get() = handOver != null

    /** Recipe packs keep their own rule (the opener may skip), so slots and hand-overs do not apply. */
    fun forRecipe(): MatchRules = copy(slots = null, handOver = null)

    companion object {
        val BUDGET_RANGE = 10..40
        val CARDS_RANGE = 6..14
        val SLOTS_RANGE = 3..7
        val TURN_SECONDS_RANGE = 5..20

        /** Five slots each and a hand-over costing 1, 2, 3… The rules the game is tested with. */
        val DEFAULT = MatchRules()

        /** No card limit and no hand-over: only the budget limits a player, and the opener must bid. */
        val CLASSIC = MatchRules(slots = null, handOver = null)
    }
}
