package com.vidanstojic.draftduel.domain.match

import com.vidanstojic.draftduel.domain.model.GameCard
import com.vidanstojic.draftduel.domain.model.Theme

/** The two sides of a match. A is always the local user in online and bot matches. */
enum class Seat {
    A,
    B,
    ;

    val other: Seat get() = if (this == A) B else A
}

data class OwnedCard(
    val card: GameCard,
    val price: Int,
)

data class SeatState(
    val name: String,
    val budget: Int,
    val cards: List<OwnedCard> = emptyList(),
    /** Hand-overs used this match; the next one costs more (see [HandOverRule]). */
    val handOvers: Int = 0,
) {
    val total: Int get() = cards.sumOf { it.card.rating }
}

enum class AwardReason {
    /** The other player passed. */
    PASSED,

    /** The bid was higher than the opponent's budget, so the card went immediately. */
    OVER_OPPONENT_BUDGET,

    /** A player with budget chose who gets the card while the other had 0 points. */
    CHOSEN,

    /** Both players had 0 points. */
    FREE,

    /** The other player's slots were full, so the card came for free (spec §3). */
    OTHER_FULL,
}

sealed interface MatchPhase {
    data class Bidding(
        val opener: Seat,
        val toAct: Seat,
        val price: Int,
        val leader: Seat?,
        /** The first opener paid to hand this card over; [opener] is now the rival and must open. */
        val handedOver: Boolean = false,
    ) : MatchPhase

    /** The opponent of [chooser] has 0 points; [chooser] decides who takes the card for free. */
    data class Choosing(val chooser: Seat) : MatchPhase

    data class Awarded(
        val winner: Seat,
        val price: Int,
        val reason: AwardReason,
    ) : MatchPhase

    /** Recipe packs: the opener skipped the card without bidding, so nobody gets it. */
    data object Discarded : MatchPhase

    data object Finished : MatchPhase
}

enum class MatchOutcome {
    A_WINS,
    B_WINS,
    DRAW,
}

data class MatchState(
    val theme: Theme,
    val pool: List<GameCard>,
    val roundIndex: Int,
    val seats: Map<Seat, SeatState>,
    val firstOpener: Seat,
    val phase: MatchPhase,
    /** The dish to build in recipe packs; null in rating packs. */
    val goal: String? = null,
    val rules: MatchRules = MatchRules.CLASSIC,
) {
    val currentCard: GameCard? get() = pool.getOrNull(roundIndex)

    /** Cards still to be drawn after the current one. */
    val cardsLeft: Int get() = (pool.size - roundIndex - 1).coerceAtLeast(0)

    val isFinished: Boolean get() = phase == MatchPhase.Finished

    /** True between rounds: the card is settled and the next one is about to be drawn. */
    val isSettled: Boolean get() = phase is MatchPhase.Awarded || phase == MatchPhase.Discarded

    fun seat(seat: Seat): SeatState = seats.getValue(seat)

    /** Empty slots of [seat], or null when the rule set has no card limit. */
    fun slotsLeft(seat: Seat): Int? = rules.slots?.let { (it - seat(seat).cards.size).coerceAtLeast(0) }

    fun isFull(seat: Seat): Boolean = slotsLeft(seat) == 0
}

sealed interface MatchAction {
    data class Raise(val seat: Seat, val step: Int) : MatchAction

    data class Pass(val seat: Seat) : MatchAction

    data class Assign(val chooser: Seat, val to: Seat) : MatchAction

    /** The opener pays to let the rival open this card instead ([MatchRules.handOver]). */
    data class HandOver(val seat: Seat) : MatchAction

    /** The player to act ran out of time. */
    data object Timeout : MatchAction
}
