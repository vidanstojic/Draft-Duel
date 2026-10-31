package com.vidanstojic.draftduel.domain.match

import com.vidanstojic.draftduel.domain.model.AvatarColorway
import com.vidanstojic.draftduel.domain.model.AvatarSpec
import com.vidanstojic.draftduel.domain.model.GameCard
import com.vidanstojic.draftduel.domain.model.Theme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** [RuleSet.FIVE_SLOTS]: five cards each and the paid hand-over (spec §3 v1 plus the hand-over rule). */
class FiveSlotsTest {

    private fun card(i: Int, rating: Int = 80) = GameCard(
        id = "c$i",
        name = "Player $i",
        season = "2025/26",
        country = "Spain",
        position = "MID",
        rating = rating,
        avatar = AvatarSpec(i, "P$i", AvatarColorway.NAVY_GOLD),
        categoryIds = emptySet(),
    )

    private fun newMatch(ruleSet: RuleSet = RuleSet.FIVE_SLOTS, ratings: List<Int> = List(10) { 80 }) = MatchEngine.start(
        theme = Theme.FOOTBALL,
        pool = ratings.mapIndexed { i, r -> card(i, r) },
        nameA = "A",
        nameB = "B",
        firstOpener = Seat.A,
        ruleSet = ruleSet,
    )

    /** Opener of the current round takes the card for 1; the other passes. */
    private fun MatchState.openerTakes(): MatchState {
        val opener = (phase as MatchPhase.Bidding).opener
        return MatchEngine.advance(MatchEngine.reduce(MatchEngine.reduce(this, MatchAction.Raise(opener, 1)), MatchAction.Pass(opener.other)))
    }

    /** The non-opener takes the card for 2: opener bids 1, the other 2, the opener passes. */
    private fun MatchState.otherTakes(): MatchState {
        val opener = (phase as MatchPhase.Bidding).opener
        var state = MatchEngine.reduce(this, MatchAction.Raise(opener, 1))
        state = MatchEngine.reduce(state, MatchAction.Raise(opener.other, 1))
        return MatchEngine.advance(MatchEngine.reduce(state, MatchAction.Pass(opener)))
    }

    @Test
    fun handOver_makesTheRivalOpen_andThePointIsGone() {
        var state = newMatch()
        assertTrue(MatchEngine.canHandOver(state, Seat.A))
        state = MatchEngine.reduce(state, MatchAction.HandOver(Seat.A))

        val phase = assertIs<MatchPhase.Bidding>(state.phase)
        assertEquals(Seat.B, phase.opener)
        assertEquals(Seat.B, phase.toAct)
        assertTrue(phase.handedOver)
        assertEquals(19, state.seat(Seat.A).budget)
        assertEquals(20, state.seat(Seat.B).budget)
    }

    @Test
    fun rivalCannotHandItBack_andMustOpen() {
        var state = MatchEngine.reduce(newMatch(), MatchAction.HandOver(Seat.A))
        assertFalse(MatchEngine.canHandOver(state, Seat.B))
        assertFalse(MatchEngine.canPass(state, Seat.B))
        assertSame(state, MatchEngine.reduce(state, MatchAction.HandOver(Seat.B)))

        state = MatchEngine.reduce(state, MatchAction.Raise(Seat.B, 1))
        state = MatchEngine.reduce(state, MatchAction.Pass(Seat.A))
        val awarded = assertIs<MatchPhase.Awarded>(state.phase)
        assertEquals(Seat.B, awarded.winner)
        assertEquals(19, state.seat(Seat.B).budget)
    }

    @Test
    fun handOverCost_risesForTheSamePlayer() {
        var state = newMatch()
        val costs = mutableListOf<Int>()
        repeat(3) {
            // A opens rounds 0, 2, 4 and hands them over (B takes them for 1); A wins B's rounds for 2.
            costs += MatchEngine.handOverCost(state, Seat.A)
            state = MatchEngine.reduce(state, MatchAction.HandOver(Seat.A))
            state = state.openerTakes()
            state = state.otherTakes()
        }
        assertEquals(listOf(1, 2, 3), costs)
        assertEquals(3, state.seat(Seat.A).handOvers)
        assertEquals(20 - (1 + 2 + 3) - 3 * 2, state.seat(Seat.A).budget)
        assertEquals(20 - 3, state.seat(Seat.B).budget)
    }

    @Test
    fun handOver_needsEnoughPoints() {
        val state = newMatch().let { it.copy(seats = it.seats + (Seat.A to it.seat(Seat.A).copy(budget = 2, handOvers = 2))) }
        assertEquals(3, MatchEngine.handOverCost(state, Seat.A))
        assertFalse(MatchEngine.canHandOver(state, Seat.A))
    }

    @Test
    fun afterTheFirstBid_thereIsNoHandOver() {
        var state = MatchEngine.reduce(newMatch(), MatchAction.Raise(Seat.A, 1))
        assertFalse(MatchEngine.canHandOver(state, Seat.B))
        assertEquals(MatchAction.Pass(Seat.B), MatchEngine.passAction(state, Seat.B))
        state = MatchEngine.reduce(newMatch(), MatchAction.Timeout)
        assertEquals(1, (state.phase as MatchPhase.Bidding).price, "a timeout still auto-bids 1 (spec §3 rule 7)")
    }

    @Test
    fun passButton_meansHandOverForTheOpener() {
        assertEquals(MatchAction.HandOver(Seat.A), MatchEngine.passAction(newMatch(), Seat.A))
        assertFalse(MatchEngine.canHandOver(newMatch(RuleSet.CLASSIC), Seat.A))
        assertEquals(MatchAction.Pass(Seat.A), MatchEngine.passAction(newMatch(RuleSet.CLASSIC), Seat.A))
    }

    @Test
    fun spec3row7_fullPlayer_restGoFreeToTheOther() {
        // B takes five cards in a row (A always passes), then the rest go to A for free.
        var state = newMatch()
        repeat(5) {
            val phase = state.phase as MatchPhase.Bidding
            state = if (phase.opener == Seat.B) {
                MatchEngine.reduce(MatchEngine.reduce(state, MatchAction.Raise(Seat.B, 1)), MatchAction.Pass(Seat.A))
            } else {
                MatchEngine.reduce(MatchEngine.reduce(state, MatchAction.Raise(Seat.A, 1)), MatchAction.Raise(Seat.B, 1))
                    .let { MatchEngine.reduce(it, MatchAction.Pass(Seat.A)) }
            }
            state = MatchEngine.advance(state)
        }
        assertTrue(state.isFull(Seat.B))
        val free = assertIs<MatchPhase.Awarded>(state.phase)
        assertEquals(Seat.A, free.winner)
        assertEquals(AwardReason.OTHER_FULL, free.reason)
        while (!state.isFinished) state = MatchEngine.advance(state)
        assertEquals(5, state.seat(Seat.A).cards.size)
        assertEquals(5, state.seat(Seat.B).cards.size)
    }

    @Test
    fun classic_hasNoCardLimit() {
        val state = newMatch(RuleSet.CLASSIC)
        assertEquals(null, state.slotsLeft(Seat.A))
        assertFalse(state.isFull(Seat.A))
    }
}
