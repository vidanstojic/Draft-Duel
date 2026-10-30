package com.vidanstojic.draftduel.domain.bot

import com.vidanstojic.draftduel.domain.match.GameRules
import com.vidanstojic.draftduel.domain.match.MatchOutcome
import com.vidanstojic.draftduel.domain.match.Seat
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Bot-vs-bot simulation on the football pack (spec §17). Seeds are fixed, so results are
 * deterministic; the thresholds leave room for re-tuning without rewriting the test.
 */
class BotSimulationTest {

    @Test
    fun everyPersonality_playsOnlyLegalMovesAndKeepsInvariants() {
        val random = Random(42)
        val personalities = BotPersonality.entries
        repeat(600) { game ->
            val a = personalities[game % personalities.size]
            val b = personalities[(game / personalities.size) % personalities.size]
            val pool = FootballPackFixture.cards.shuffled(random).take(GameRules.POOL_SIZE)
            val result = BotSimulation.play(
                botA = BotFactory.create(a, FootballPackFixture.stats, Random(random.nextInt())),
                botB = BotFactory.create(b, FootballPackFixture.stats, Random(random.nextInt())),
                pool = pool,
                firstOpener = if (game % 2 == 0) Seat.A else Seat.B,
            )

            assertEquals(0, result.rejectedActions, "$a vs $b made an illegal move")
            assertEquals(0, result.timeouts, "$a vs $b let the clock run out")
            val seats = result.final.seats.values
            assertEquals(GameRules.POOL_SIZE, seats.sumOf { it.cards.size })
            seats.forEach { seat ->
                assertTrue(seat.budget >= 0)
                assertEquals(GameRules.STARTING_BUDGET - seat.budget, seat.cards.sumOf { it.price })
            }
        }
    }

    @Test
    fun firstOpener_hasNoBigAdvantage() {
        val random = Random(7)
        var openerPoints = 0.0
        val games = 1_000
        repeat(games) { game ->
            val pool = FootballPackFixture.cards.shuffled(random).take(GameRules.POOL_SIZE)
            val result = BotSimulation.play(
                botA = BotFactory.create(BotPersonality.entries.random(random), FootballPackFixture.stats, Random(random.nextInt())),
                botB = BotFactory.create(BotPersonality.entries.random(random), FootballPackFixture.stats, Random(random.nextInt())),
                pool = pool,
                firstOpener = Seat.A,
            )
            openerPoints += when (result.outcome) {
                MatchOutcome.A_WINS -> 1.0
                MatchOutcome.DRAW -> 0.5
                MatchOutcome.B_WINS -> 0.0
            }
        }
        val rate = openerPoints / games
        // Spec §17 target is 45-55 %; the band is a little wider for a 1 000-game sample.
        assertTrue(rate in 0.42..0.58, "first opener won $rate")
    }

    @Test
    fun difficultyTiers_areOrdered() {
        val all = BotPersonality.entries
        val average = all.associateWith { a ->
            all.filter { it != a }.map { b -> BotSimulation.winRate(a, b, games = 200, seed = a.ordinal * 31 + b.ordinal) }.average()
        }
        fun tier(d: BotDifficulty) = all.filter { it.difficulty == d }.map { average.getValue(it) }.average()

        val easy = tier(BotDifficulty.EASY)
        val medium = tier(BotDifficulty.MEDIUM)
        val hard = tier(BotDifficulty.HARD)
        assertTrue(easy < medium && medium < hard, "tiers out of order: easy $easy, medium $medium, hard $hard")
    }

    @Test
    fun scout_beatsCasualFanMostOfTheTime() {
        val rate = BotSimulation.winRate(BotPersonality.SCOUT, BotPersonality.CASUAL_FAN, games = 300, seed = 1)
        assertTrue(rate > 0.8, "Scout won only $rate against Casual fan")
    }

    @Test
    fun quickMatch_canMeetEveryPersonality() {
        val random = Random(3)
        val seen = (1..500).map { BotPersonality.randomForQuickMatch(random) }.toSet()
        assertEquals(BotPersonality.entries.toSet(), seen)
    }
}
