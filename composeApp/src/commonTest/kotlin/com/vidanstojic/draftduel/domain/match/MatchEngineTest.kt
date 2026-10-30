package com.vidanstojic.draftduel.domain.match

import com.vidanstojic.draftduel.domain.model.AthleteCard
import com.vidanstojic.draftduel.domain.model.AvatarColorway
import com.vidanstojic.draftduel.domain.model.AvatarSpec
import com.vidanstojic.draftduel.domain.model.Theme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Rows of the example table in spec §3, adapted to the rules without the 5-slot limit. */
class MatchEngineTest {

    private fun card(i: Int, rating: Int = 80) = AthleteCard(
        id = "c$i",
        name = "Player $i",
        season = "2020/21",
        country = "Spain",
        position = "MID",
        rating = rating,
        avatar = AvatarSpec(i, "P$i", AvatarColorway.NAVY_GOLD),
        categoryIds = emptySet(),
    )

    private fun newMatch(cards: Int = GameRules.POOL_SIZE) = MatchEngine.start(
        theme = Theme.FOOTBALL,
        pool = List(cards) { card(it) },
        nameA = "A",
        nameB = "B",
        firstOpener = Seat.A,
    )

    private fun MatchState.withBudgets(a: Int, b: Int) = copy(
        seats = mapOf(Seat.A to seat(Seat.A).copy(budget = a), Seat.B to seat(Seat.B).copy(budget = b)),
    )

    @Test
    fun openerBidsOneAndOpponentPasses_openerWinsForOne() {
        var state = newMatch()
        state = MatchEngine.reduce(state, MatchAction.Raise(Seat.A, 1))
        state = MatchEngine.reduce(state, MatchAction.Pass(Seat.B))

        val phase = assertIs<MatchPhase.Awarded>(state.phase)
        assertEquals(Seat.A, phase.winner)
        assertEquals(19, state.seat(Seat.A).budget)
    }

    @Test
    fun openerCannotPassWithoutBid() {
        val state = newMatch()
        assertSame(state, MatchEngine.reduce(state, MatchAction.Pass(Seat.A)))
    }

    @Test
    fun bidOverOpponentBudget_isAwardedImmediately() {
        var state = newMatch().withBudgets(a = 20, b = 6)
        state = MatchEngine.reduce(state, MatchAction.Raise(Seat.A, 5))
        state = MatchEngine.reduce(state, MatchAction.Raise(Seat.B, 1))
        state = MatchEngine.reduce(state, MatchAction.Raise(Seat.A, 1))

        val phase = assertIs<MatchPhase.Awarded>(state.phase)
        assertEquals(Seat.A, phase.winner)
        assertEquals(7, phase.price)
        assertEquals(AwardReason.OVER_OPPONENT_BUDGET, phase.reason)
    }

    @Test
    fun timeoutAutoRaisesByOne() {
        var state = newMatch().withBudgets(a = 20, b = 9)
        state = MatchEngine.reduce(state, MatchAction.Raise(Seat.A, 5))
        state = MatchEngine.reduce(state, MatchAction.Timeout)

        val phase = assertIs<MatchPhase.Bidding>(state.phase)
        assertEquals(6, phase.price)
        assertEquals(Seat.B, phase.leader)
    }

    @Test
    fun timeoutOverBudget_countsAsPass() {
        var state = newMatch().withBudgets(a = 20, b = 9)
        state = MatchEngine.reduce(state, MatchAction.Raise(Seat.A, 5))
        state = MatchEngine.reduce(state, MatchAction.Raise(Seat.B, 2))
        state = MatchEngine.reduce(state, MatchAction.Raise(Seat.A, 2))
        state = MatchEngine.reduce(state, MatchAction.Timeout)

        val phase = assertIs<MatchPhase.Awarded>(state.phase)
        assertEquals(Seat.A, phase.winner)
        assertEquals(9, phase.price)
    }

    @Test
    fun playerOnZero_opponentChooses() {
        var state = newMatch().withBudgets(a = 0, b = 7)
        state = state.copy(phase = MatchPhase.Awarded(Seat.B, 0, AwardReason.FREE), roundIndex = 0)
        state = MatchEngine.advance(state)

        val phase = assertIs<MatchPhase.Choosing>(state.phase)
        assertEquals(Seat.B, phase.chooser)
    }

    @Test
    fun choosingTimeout_cardGoesToPlayerOnZero() {
        var state = newMatch().withBudgets(a = 0, b = 7).copy(phase = MatchPhase.Choosing(Seat.B))
        state = MatchEngine.reduce(state, MatchAction.Timeout)

        val phase = assertIs<MatchPhase.Awarded>(state.phase)
        assertEquals(Seat.A, phase.winner)
        assertEquals(0, phase.price)
    }

    @Test
    fun bothOnZero_cardGoesFreeToPlayerWithFewerCards() {
        var state = newMatch().withBudgets(a = 0, b = 0)
        state = state.copy(phase = MatchPhase.Awarded(Seat.A, 0, AwardReason.FREE))
        state = state.copy(seats = state.seats + (Seat.B to state.seat(Seat.B).copy(cards = listOf(OwnedCard(card(99), 3)))))
        state = MatchEngine.advance(state)

        val phase = assertIs<MatchPhase.Awarded>(state.phase)
        assertEquals(Seat.A, phase.winner)
        assertEquals(AwardReason.FREE, phase.reason)
    }

    @Test
    fun equalTotals_isDraw() {
        val state = newMatch().copy(
            seats = mapOf(
                Seat.A to SeatState("A", 0, listOf(OwnedCard(card(1, 90), 5))),
                Seat.B to SeatState("B", 0, listOf(OwnedCard(card(2, 90), 5))),
            ),
        )
        assertEquals(MatchOutcome.DRAW, MatchEngine.outcome(state))
    }

    @Test
    fun matchEndsAfterEveryCardIsAwarded_andBudgetsStayNonNegative() {
        var state = newMatch()
        repeat(100) {
            state = when (state.phase) {
                is MatchPhase.Awarded -> MatchEngine.advance(state)
                MatchPhase.Finished -> state
                else -> MatchEngine.reduce(state, MatchAction.Timeout)
            }
        }
        assertTrue(state.isFinished)
        assertEquals(GameRules.POOL_SIZE, state.seat(Seat.A).cards.size + state.seat(Seat.B).cards.size)
        assertTrue(state.seat(Seat.A).budget >= 0 && state.seat(Seat.B).budget >= 0)
    }

    @Test
    fun openerAlternatesEveryRound() {
        val state = newMatch()
        assertEquals(Seat.A, MatchEngine.openerFor(state, 0))
        assertEquals(Seat.B, MatchEngine.openerFor(state, 1))
        assertEquals(Seat.A, MatchEngine.openerFor(state, 2))
    }
}
