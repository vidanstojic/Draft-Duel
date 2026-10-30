package com.vidanstojic.draftduel.domain.bot

import com.vidanstojic.draftduel.domain.match.MatchAction
import com.vidanstojic.draftduel.domain.match.MatchEngine
import com.vidanstojic.draftduel.domain.match.MatchPhase
import com.vidanstojic.draftduel.domain.match.MatchState
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.model.Scoring
import com.vidanstojic.draftduel.domain.model.Theme
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersonalityBotTest {

    private val stats = PoolStats.of(SimPacks.football.cards, Scoring.RATINGS)

    private fun match(cards: Int = 10, opener: Seat = Seat.B): MatchState =
        MatchEngine.start(Theme.FOOTBALL, SimPacks.football.cards.take(cards), "A", "B", opener)

    private fun MatchState.withBudgets(a: Int, b: Int) = copy(
        seats = mapOf(Seat.A to seat(Seat.A).copy(budget = a), Seat.B to seat(Seat.B).copy(budget = b)),
    )

    private fun bot(valuation: Valuation, style: BidStyle = BidStyle(), endgame: Boolean = true) = PersonalityBot(
        behavior = BotBehavior(RatingPerception(noise = 0), valuation, style, playsEndgame = endgame),
        stats = stats,
        random = Random(0),
    )

    @Test
    fun notItsTurn_returnsNull() {
        val state = match(opener = Seat.A)
        assertNull(bot(valuation = { 5 }).decide(state, Seat.B))
    }

    @Test
    fun opener_bidsEvenWhenItWantsNothing() {
        val action = bot(valuation = { 0 }).decide(match(opener = Seat.B), Seat.B)
        assertEquals(MatchAction.Raise(Seat.B, 1), action)
    }

    @Test
    fun belowLimit_raisesAndAtLimit_passes() {
        var state = MatchEngine.reduce(match(opener = Seat.A), MatchAction.Raise(Seat.A, 1))
        assertEquals(MatchAction.Raise(Seat.B, 1), bot(valuation = { 2 }).decide(state, Seat.B))

        state = MatchEngine.reduce(state, MatchAction.Raise(Seat.B, 1))
        state = MatchEngine.reduce(state, MatchAction.Raise(Seat.A, 1))
        assertEquals(MatchAction.Pass(Seat.B), bot(valuation = { 2 }).decide(state, Seat.B))
    }

    @Test
    fun sniper_jumpsByFiveWhenThatWinsOutright() {
        var state = match(opener = Seat.A).withBudgets(a = 5, b = 20)
        state = MatchEngine.reduce(state, MatchAction.Raise(Seat.A, 1))
        // Rival has 5 points: 1 + 5 = 6 wins outright, where a plain bidder would raise by 1.
        val action = bot(valuation = { 10 }, style = BidStyle(snipes = true)).decide(state, Seat.B)
        assertEquals(MatchAction.Raise(Seat.B, 5), action)
        val awarded = assertIs<MatchPhase.Awarded>(MatchEngine.reduce(state, assertNotNull(action)).phase)
        assertEquals(Seat.B, awarded.winner)
        assertEquals(MatchAction.Raise(Seat.B, 1), bot(valuation = { 10 }).decide(state, Seat.B))
    }

    @Test
    fun endgame_spendsWholeBudgetOnLastCard() {
        val state = match(cards = 1, opener = Seat.B).withBudgets(a = 20, b = 20)
        val bot = bot(valuation = { 1 })
        assertEquals(20, bot.limit(state, Seat.B))
        assertEquals(1, bot(valuation = { 1 }, endgame = false).limit(state, Seat.B))
    }

    @Test
    fun endgame_neverBidsPastRivalBudgetPlusOne() {
        val state = match().withBudgets(a = 4, b = 20)
        assertEquals(5, bot(valuation = { 15 }).limit(state, Seat.B))
    }

    @Test
    fun chooser_alwaysTakesTheFreeCard() {
        val choosing = match().withBudgets(a = 0, b = 6).copy(phase = MatchPhase.Choosing(chooser = Seat.B))
        assertEquals(MatchAction.Assign(chooser = Seat.B, to = Seat.B), bot(valuation = { 0 }).decide(choosing, Seat.B))
    }

    @Test
    fun perception_isStableWithinARound() {
        val bot = BotFactory.create(BotPersonality.CASUAL_FAN, stats, Random(5)) as PersonalityBot
        val state = match()
        val first = bot.perceivedRating(state)
        repeat(5) { assertEquals(first, bot.perceivedRating(state)) }
    }

    @Test
    fun lookahead_isSymmetricAndBounded() {
        val lookahead = AuctionLookahead(stats)
        for (b in listOf(1, 5, 12, 20)) for (o in listOf(1, 7, 20)) for (n in listOf(1, 4, 9)) {
            assertTrue(abs(lookahead.value(b, o, n) + lookahead.value(o, b, n)) < 1e-6, "V($b,$o,$n) not antisymmetric")
            val reservation = lookahead.reservation(rating = 85.0, budget = b, opponentBudget = o, cardsAfter = n)
            assertTrue(reservation in 0..minOf(b, o + 1))
        }
        assertEquals(0.0, lookahead.value(20, 20, 10), 1e-9)
    }

    @Test
    fun lookahead_spendsEverythingOnTheLastCard() {
        val lookahead = AuctionLookahead(stats)
        assertEquals(8, lookahead.reservation(rating = 70.0, budget = 8, opponentBudget = 15, cardsAfter = 0))
    }
}
