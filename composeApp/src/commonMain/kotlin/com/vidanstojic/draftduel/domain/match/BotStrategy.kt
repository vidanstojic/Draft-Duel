package com.vidanstojic.draftduel.domain.match

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * On-device opponent (spec §12), adapted to hidden ratings: the bot does not see the real rating,
 * it bids on a noisy guess so it plays like a person who roughly knows the athlete.
 */
class BotStrategy(
    private val averageRating: Double,
    private val random: Random = Random.Default,
) {

    fun decide(state: MatchState, seat: Seat): MatchAction? = when (val phase = state.phase) {
        is MatchPhase.Bidding -> if (phase.toAct == seat) bid(state, seat, phase) else null
        is MatchPhase.Choosing -> if (phase.chooser == seat) choose(state, seat) else null
        else -> null
    }

    /** 1–3 s, so the bot feels human. */
    fun thinkTimeMillis(): Long = random.nextLong(from = 1_000, until = 3_000)

    private fun bid(state: MatchState, seat: Seat, phase: MatchPhase.Bidding): MatchAction {
        if (phase.leader == null) return MatchAction.Raise(seat, 1)
        val maxPrice = maxPrice(state, seat)
        return if (phase.price + 1 <= maxPrice && MatchEngine.canRaise(state, seat, 1)) {
            MatchAction.Raise(seat, 1)
        } else {
            MatchAction.Pass(seat)
        }
    }

    private fun choose(state: MatchState, seat: Seat): MatchAction {
        val takeIt = estimatedRating(state) > averageRating
        return MatchAction.Assign(chooser = seat, to = if (takeIt) seat else seat.other)
    }

    /** p_max = min(B, floor(B / S * (r - 60) / (avg - 60) * eps)), with S = the bot's fair share of the remaining cards. */
    private fun maxPrice(state: MatchState, seat: Seat): Int {
        val budget = state.seat(seat).budget
        val remaining = state.pool.size - state.roundIndex
        val share = max(1, (remaining + 1) / 2)
        val strength = (estimatedRating(state) - GameRules.MIN_RATING) / (averageRating - GameRules.MIN_RATING)
        val eps = 0.8 + random.nextDouble() * 0.4
        val raw = floor(budget.toDouble() / share * strength * eps).toInt()
        return min(budget, max(0, raw))
    }

    private fun estimatedRating(state: MatchState): Double {
        val rating = state.currentCard?.rating ?: GameRules.MIN_RATING
        return (rating + random.nextInt(from = -6, until = 7)).toDouble()
    }
}
