package com.vidanstojic.draftduel.domain.match

import com.vidanstojic.draftduel.domain.model.GameCard
import com.vidanstojic.draftduel.domain.model.Scoring
import com.vidanstojic.draftduel.domain.model.Theme

/**
 * Pure, deterministic match rules used by the UI demo and the on-device bot match.
 *
 * Planned to move into the shared `:game-core` module (spec §8). Invalid actions return the state
 * unchanged, the same way the server answers with an error and no state change.
 */
object MatchEngine {

    fun start(
        theme: Theme,
        pool: List<GameCard>,
        nameA: String,
        nameB: String,
        firstOpener: Seat,
        goal: String? = null,
        ruleSet: RuleSet = RuleSet.CLASSIC,
    ): MatchState {
        require(pool.isNotEmpty()) { "A match needs at least one card." }
        val initial = MatchState(
            theme = theme,
            pool = pool,
            roundIndex = 0,
            seats = mapOf(
                Seat.A to SeatState(nameA, GameRules.STARTING_BUDGET),
                Seat.B to SeatState(nameB, GameRules.STARTING_BUDGET),
            ),
            firstOpener = firstOpener,
            phase = MatchPhase.Finished,
            goal = goal,
            ruleSet = ruleSet,
        )
        return drawRound(initial)
    }

    fun reduce(state: MatchState, action: MatchAction): MatchState = when (action) {
        is MatchAction.Raise -> raise(state, action.seat, action.step)
        is MatchAction.Pass -> pass(state, action.seat)
        is MatchAction.Assign -> assign(state, action.chooser, action.to)
        is MatchAction.HandOver -> handOver(state, action.seat)
        MatchAction.Timeout -> timeout(state)
    }

    /** Moves from a settled card ([MatchPhase.Awarded] or [MatchPhase.Discarded]) to the next one, or to [MatchPhase.Finished]. */
    fun advance(state: MatchState): MatchState {
        if (!state.isSettled) return state
        val next = state.roundIndex + 1
        if (next >= state.pool.size) return state.copy(roundIndex = state.pool.size, phase = MatchPhase.Finished)
        return drawRound(state.copy(roundIndex = next))
    }

    fun canRaise(state: MatchState, seat: Seat, step: Int): Boolean {
        val phase = state.phase as? MatchPhase.Bidding ?: return false
        return phase.toAct == seat && step in GameRules.BID_STEPS && phase.price + step <= state.seat(seat).budget
    }

    fun canPass(state: MatchState, seat: Seat): Boolean {
        val phase = state.phase as? MatchPhase.Bidding ?: return false
        return phase.toAct == seat && (phase.leader != null || openerMaySkip(state))
    }

    /**
     * The opener, before any bid, may pay [handOverCost] to make the rival open instead. Once per
     * card, only in rule sets that allow it, and only with enough points to pay.
     */
    fun canHandOver(state: MatchState, seat: Seat): Boolean {
        if (!state.ruleSet.allowsHandOver) return false
        val phase = state.phase as? MatchPhase.Bidding ?: return false
        return phase.toAct == seat && phase.opener == seat && phase.leader == null && !phase.handedOver &&
            state.seat(seat).budget >= handOverCost(state, seat)
    }

    /** What the pass button means: a paid hand-over for an opener who may, otherwise a plain pass. */
    fun passAction(state: MatchState, seat: Seat): MatchAction =
        if (canHandOver(state, seat)) MatchAction.HandOver(seat) else MatchAction.Pass(seat)

    /** 1 for the first hand-over of the match, then 2, 3… for the same player. */
    fun handOverCost(state: MatchState, seat: Seat): Int = state.seat(seat).handOvers + 1

    /** Spec §3 rule 3 (the opener must bid) holds for rating packs; in recipe packs the opener may skip. */
    fun openerMaySkip(state: MatchState): Boolean = state.theme.scoring == Scoring.RECIPE

    fun outcome(state: MatchState): MatchOutcome {
        val a = state.seat(Seat.A).total
        val b = state.seat(Seat.B).total
        return when {
            a > b -> MatchOutcome.A_WINS
            b > a -> MatchOutcome.B_WINS
            else -> MatchOutcome.DRAW
        }
    }

    fun openerFor(state: MatchState, roundIndex: Int): Seat =
        if (roundIndex % 2 == 0) state.firstOpener else state.firstOpener.other

    private fun drawRound(state: MatchState): MatchState {
        val budgetA = state.seat(Seat.A).budget
        val budgetB = state.seat(Seat.B).budget
        val opener = openerFor(state, state.roundIndex)
        val fullA = state.isFull(Seat.A)
        val fullB = state.isFull(Seat.B)
        return when {
            fullA && fullB -> state.copy(phase = MatchPhase.Finished)
            fullA -> award(state, winner = Seat.B, price = 0, reason = AwardReason.OTHER_FULL)
            fullB -> award(state, winner = Seat.A, price = 0, reason = AwardReason.OTHER_FULL)
            budgetA > 0 && budgetB > 0 ->
                state.copy(phase = MatchPhase.Bidding(opener = opener, toAct = opener, price = 0, leader = null))
            budgetA > 0 -> state.copy(phase = MatchPhase.Choosing(chooser = Seat.A))
            budgetB > 0 -> state.copy(phase = MatchPhase.Choosing(chooser = Seat.B))
            else -> award(state, winner = seatWithFewerCards(state, tieBreaker = opener), price = 0, reason = AwardReason.FREE)
        }
    }

    private fun raise(state: MatchState, seat: Seat, step: Int): MatchState {
        val phase = state.phase as? MatchPhase.Bidding ?: return state
        if (!canRaise(state, seat, step)) return state
        val newPrice = phase.price + step
        if (newPrice > state.seat(seat.other).budget) {
            return award(state, winner = seat, price = newPrice, reason = AwardReason.OVER_OPPONENT_BUDGET)
        }
        return state.copy(phase = phase.copy(toAct = seat.other, price = newPrice, leader = seat))
    }

    private fun pass(state: MatchState, seat: Seat): MatchState {
        val phase = state.phase as? MatchPhase.Bidding ?: return state
        if (!canPass(state, seat)) return state
        val leader = phase.leader ?: return state.copy(phase = MatchPhase.Discarded)
        return award(state, winner = leader, price = phase.price, reason = AwardReason.PASSED)
    }

    private fun handOver(state: MatchState, seat: Seat): MatchState {
        val phase = state.phase as? MatchPhase.Bidding ?: return state
        if (!canHandOver(state, seat)) return state
        val payer = state.seat(seat)
        val paid = payer.copy(budget = payer.budget - handOverCost(state, seat), handOvers = payer.handOvers + 1)
        return state.copy(
            seats = state.seats + (seat to paid),
            phase = phase.copy(opener = seat.other, toAct = seat.other, handedOver = true),
        )
    }

    private fun assign(state: MatchState, chooser: Seat, to: Seat): MatchState {
        val phase = state.phase as? MatchPhase.Choosing ?: return state
        if (phase.chooser != chooser) return state
        return award(state, winner = to, price = 0, reason = AwardReason.CHOSEN)
    }

    /** Spec §3 rule 7: auto-raise by 1, or pass if that is over budget. Choosing: card goes to the 0-point player. */
    private fun timeout(state: MatchState): MatchState = when (val phase = state.phase) {
        is MatchPhase.Bidding -> {
            val seat = phase.toAct
            when {
                canRaise(state, seat, 1) -> raise(state, seat, 1)
                phase.leader != null -> award(state, phase.leader, phase.price, AwardReason.PASSED)
                else -> state
            }
        }
        is MatchPhase.Choosing -> award(state, phase.chooser.other, price = 0, reason = AwardReason.CHOSEN)
        else -> state
    }

    private fun award(state: MatchState, winner: Seat, price: Int, reason: AwardReason): MatchState {
        val card = state.currentCard ?: return state
        val seat = state.seat(winner)
        val updated = seat.copy(
            budget = seat.budget - price,
            cards = seat.cards + OwnedCard(card, price),
        )
        return state.copy(
            seats = state.seats + (winner to updated),
            phase = MatchPhase.Awarded(winner = winner, price = price, reason = reason),
        )
    }

    private fun seatWithFewerCards(state: MatchState, tieBreaker: Seat): Seat {
        val a = state.seat(Seat.A).cards.size
        val b = state.seat(Seat.B).cards.size
        return when {
            a < b -> Seat.A
            b < a -> Seat.B
            else -> tieBreaker
        }
    }
}
