package com.vidanstojic.draftduel.domain.match

/** Match constants (spec §3, without the 5-slot limit removed after UI feedback). */
object GameRules {
    const val STARTING_BUDGET = 20
    const val POOL_SIZE = 10
    const val TURN_SECONDS = 10
    const val AWARD_PAUSE_MILLIS = 2_000L
    const val MIN_RATING = 60
    val BID_STEPS: List<Int> = listOf(1, 2, 5)
}
