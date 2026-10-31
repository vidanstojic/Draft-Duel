package com.vidanstojic.draftduel.domain.match

import com.vidanstojic.draftduel.domain.bot.BotFactory
import com.vidanstojic.draftduel.domain.bot.BotPersonality
import com.vidanstojic.draftduel.domain.bot.BotSimulation
import com.vidanstojic.draftduel.domain.bot.PersonalityBot
import com.vidanstojic.draftduel.domain.bot.PoolStats
import com.vidanstojic.draftduel.domain.bot.SimPacks
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Scoring
import com.vidanstojic.draftduel.domain.model.Theme
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** [MatchRules] values other than the two presets, as set in the Custom editor. */
class CustomRulesTest {

    private fun start(rules: MatchRules, cards: Int = rules.cards) = MatchEngine.start(
        theme = Theme.FOOTBALL,
        pool = SimPacks.football.drawPool(PoolSource.Random, cards, Random(1)).cards,
        nameA = "A",
        nameB = "B",
        firstOpener = Seat.A,
        rules = rules,
    )

    @Test
    fun defaultPreset_isFiveSlotsWithRisingHandOver() {
        val rules = MatchRules.DEFAULT
        assertEquals(20, rules.budget)
        assertEquals(10, rules.cards)
        assertEquals(5, rules.slots)
        assertEquals(listOf(1, 2, 3), (0..2).map { rules.handOver!!.cost(it) })
        assertEquals(null, MatchRules.CLASSIC.slots)
        assertEquals(null, MatchRules.CLASSIC.handOver)
    }

    @Test
    fun budget_andCardCount_comeFromTheRules() {
        val rules = MatchRules(budget = 30, cards = 12, slots = 6)
        val state = start(rules)
        assertEquals(30, state.seat(Seat.A).budget)
        assertEquals(12, state.pool.size)
        assertEquals(6, state.slotsLeft(Seat.B))
    }

    @Test
    fun flatHandOverCost_doesNotRise() {
        var state = start(MatchRules(handOver = HandOverRule(firstCost = 2, increment = 0)))
        assertEquals(2, MatchEngine.handOverCost(state, Seat.A))
        state = MatchEngine.reduce(state, MatchAction.HandOver(Seat.A))
        assertEquals(18, state.seat(Seat.A).budget)
        assertEquals(2, MatchEngine.handOverCost(state, Seat.A))
    }

    @Test
    fun valuesOutsideTheRanges_areRejected() {
        assertFailsWith<IllegalArgumentException> { MatchRules(budget = 5) }
        assertFailsWith<IllegalArgumentException> { MatchRules(slots = 9) }
        assertFailsWith<IllegalArgumentException> { MatchRules(turnSeconds = 60) }
        assertFailsWith<IllegalArgumentException> { HandOverRule(firstCost = 0) }
    }

    @Test
    fun fewSlotsAndManyCards_endWhenBothAreFull() {
        val random = Random(4)
        val rules = MatchRules(cards = 14, slots = 4)
        repeat(50) {
            val result = BotSimulation.playOnce(BotPersonality.STEADY, BotPersonality.SCOUT, SimPacks.football, random, Seat.A, rules)
            assertTrue(result.final.isFinished)
            assertEquals(listOf(4, 4), result.final.seats.values.map { it.cards.size })
            assertEquals(0, result.rejectedActions)
        }
    }

    @Test
    fun publicRatings_botsSeeTheExactRating() {
        val state = start(MatchRules(ratingsVisible = true))
        val bot = BotFactory.create(BotPersonality.CASUAL_FAN, PoolStats.of(SimPacks.football.cards, Scoring.RATINGS), Random(1)) as PersonalityBot
        assertEquals(state.currentCard!!.rating.toDouble(), bot.perceivedRating(state))
    }

    @Test
    fun everyBot_playsLegallyWithBigCustomRules() {
        val random = Random(8)
        val rules = MatchRules(budget = 40, cards = 14, slots = 7, handOver = HandOverRule(firstCost = 3, increment = 2), turnSeconds = 5)
        for (personality in BotPersonality.entries) {
            val result = BotSimulation.playOnce(personality, BotPersonality.SCOUT, SimPacks.movies, random, Seat.A, rules)
            assertEquals(0, result.rejectedActions, "$personality made an illegal move")
            assertEquals(0, result.timeouts, "$personality let the clock run out")
            assertIs<MatchPhase.Finished>(result.final.phase)
        }
    }

    @Test
    fun recipeThemes_dropSlotsAndHandOvers() {
        val rules = MatchRules.DEFAULT.forRecipe()
        assertEquals(null, rules.slots)
        assertEquals(null, rules.handOver)
        assertEquals(MatchRules.DEFAULT.budget, rules.budget)
    }
}
