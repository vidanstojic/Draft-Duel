package com.vidanstojic.draftduel.domain.match

import com.vidanstojic.draftduel.data.pack.PackJson
import com.vidanstojic.draftduel.domain.bot.BidStyle
import com.vidanstojic.draftduel.domain.bot.BotBehavior
import com.vidanstojic.draftduel.domain.bot.PersonalityBot
import com.vidanstojic.draftduel.domain.bot.PoolStats
import com.vidanstojic.draftduel.domain.bot.RatingPerception
import com.vidanstojic.draftduel.domain.bot.SpecFormulaValuation
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Scoring
import com.vidanstojic.draftduel.domain.model.Theme
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Food packs: one dish per match, fitting ingredients sum to 100, wrong ones cost points. */
class RecipeModeTest {

    private val pack = PackJson.decode(
        """
        {"id": "food", "version": 1, "theme": "Food", "athletes": [
          {"id": "p1", "name": "Pizza dough", "rating": 30, "categories": ["Pizza"], "position": "BASE"},
          {"id": "p2", "name": "Tomato sauce", "rating": 25, "categories": ["Pizza"], "position": "SAUCE"},
          {"id": "p3", "name": "Mozzarella", "rating": 25, "categories": ["Pizza"], "position": "DAIRY"},
          {"id": "p4", "name": "Basil", "rating": 10, "categories": ["Pizza"], "position": "HERB"},
          {"id": "p5", "name": "Olive oil", "rating": 5, "categories": ["Pizza"], "position": "FAT"},
          {"id": "p6", "name": "Salt", "rating": 5, "categories": ["Pizza"], "position": "SPICE"},
          {"id": "p7", "name": "Pineapple", "rating": -15, "categories": ["Pizza"], "position": "FRUIT"},
          {"id": "p8", "name": "Ketchup", "rating": -10, "categories": ["Pizza"], "position": "SAUCE"},
          {"id": "p9", "name": "Chocolate", "rating": -20, "categories": ["Pizza"], "position": "SWEET"},
          {"id": "p10", "name": "Cheddar", "rating": -5, "categories": ["Pizza"], "position": "DAIRY"},
          {"id": "p11", "name": "Cucumber", "rating": -10, "categories": ["Pizza"], "position": "VEG"},
          {"id": "t1", "name": "Too small", "rating": 50, "categories": ["Toast"]}
        ]}
        """.trimIndent(),
        Theme.FOOD,
    )

    private fun match(opener: Seat = Seat.A): MatchState {
        val pool = pack.drawPool(PoolSource.ByCategory("pizza"), random = Random(1))
        return MatchEngine.start(Theme.FOOD, pool.cards, "A", "B", opener, goal = pool.goal?.name)
    }

    @Test
    fun pool_isOneDishWithEveryFittingIngredientAndSomeWrongOnes() {
        val pool = pack.drawPool(PoolSource.ByCategory("pizza"), random = Random(2))
        assertEquals("Pizza", pool.goal?.name)
        assertEquals(GameRules.POOL_SIZE, pool.cards.size)
        assertEquals(100, pool.cards.filter { it.rating > 0 }.sumOf { it.rating })
        assertEquals(4, pool.cards.count { it.rating < 0 })
        assertEquals(11, pool.drawnFrom.size)
    }

    @Test
    fun randomSource_picksADishThatCanFillAPool() {
        val pool = pack.drawPool(PoolSource.Random, random = Random(3))
        assertEquals("Pizza", pool.goal?.name, "Toast has one card and cannot be played")
    }

    @Test
    fun opener_maySkip_andNobodyGetsTheCard() {
        var state = match()
        assertTrue(MatchEngine.canPass(state, Seat.A))
        state = MatchEngine.reduce(state, MatchAction.Pass(Seat.A))
        assertSame(MatchPhase.Discarded, state.phase)

        state = MatchEngine.advance(state)
        assertIs<MatchPhase.Bidding>(state.phase)
        assertEquals(1, state.roundIndex)
        assertTrue(state.seats.values.all { it.cards.isEmpty() && it.budget == GameRules.STARTING_BUDGET })
    }

    @Test
    fun wrongIngredient_lowersTheTotal() {
        var state = match()
        val wrongIndex = state.pool.indexOfFirst { it.rating < 0 }
        state = state.copy(roundIndex = wrongIndex, phase = MatchPhase.Bidding(Seat.A, Seat.A, price = 0, leader = null))
        state = MatchEngine.reduce(state, MatchAction.Raise(Seat.A, 1))
        state = MatchEngine.reduce(state, MatchAction.Pass(Seat.B))
        assertTrue(state.seat(Seat.A).total < 0)
    }

    @Test
    fun ratingPacks_stillMakeTheOpenerBid() {
        val football = MatchEngine.start(Theme.FOOTBALL, pack.cards.map { it.copy(rating = 80) }.take(10), "A", "B", Seat.A)
        assertTrue(!MatchEngine.canPass(football, Seat.A))
    }

    @Test
    fun bot_skipsWrongIngredientsAndGivesThemAwayWhenChoosing() {
        val stats = PoolStats.of(pack.cardsIn("pizza"), Scoring.RECIPE)
        val bot = PersonalityBot(BotBehavior(RatingPerception(noise = 0), SpecFormulaValuation(), BidStyle()), stats, Random(0))
        val base = match()
        val wrong = base.pool.indexOfFirst { it.rating < 0 }
        val good = base.pool.indexOfFirst { it.rating >= 20 }

        val opening = base.copy(roundIndex = wrong, phase = MatchPhase.Bidding(Seat.A, Seat.A, price = 0, leader = null))
        assertEquals(MatchAction.Pass(Seat.A), bot.decide(opening, Seat.A))
        assertEquals(0, bot.limit(opening.copy(roundIndex = wrong), Seat.A))

        val choosing = base.copy(roundIndex = wrong, phase = MatchPhase.Choosing(chooser = Seat.A))
        assertEquals(MatchAction.Assign(chooser = Seat.A, to = Seat.B), bot.decide(choosing, Seat.A))
        val choosingGood = base.copy(roundIndex = good, phase = MatchPhase.Choosing(chooser = Seat.A))
        assertEquals(MatchAction.Assign(chooser = Seat.A, to = Seat.A), bot.decide(choosingGood, Seat.A))

        val openingGood = base.copy(roundIndex = good, phase = MatchPhase.Bidding(Seat.A, Seat.A, price = 0, leader = null))
        assertIs<MatchAction.Raise>(bot.decide(openingGood, Seat.A))
    }

    @Test
    fun recipeValues_mayBeNegative_butRatingPacksDropThem() {
        assertEquals(11, pack.cardsIn("pizza").size)
        val football = PackJson.decode(
            """{"id": "f", "version": 1, "theme": "Football", "athletes": [{"id": "x", "name": "X", "rating": -5}]}""",
            Theme.FOOTBALL,
        )
        assertTrue(football.cards.isEmpty())
        assertNull(pack.cards.firstOrNull { it.rating !in -100..100 })
    }
}
