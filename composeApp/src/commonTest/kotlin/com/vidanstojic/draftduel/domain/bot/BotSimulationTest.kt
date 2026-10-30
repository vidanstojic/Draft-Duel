package com.vidanstojic.draftduel.domain.bot

import com.vidanstojic.draftduel.domain.match.GameRules
import com.vidanstojic.draftduel.domain.match.MatchOutcome
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.model.Scoring
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Bot-vs-bot simulation on the football, movies and food packs (spec §17). Seeds are fixed, so
 * results are deterministic; the thresholds leave room for re-tuning without rewriting the test.
 */
class BotSimulationTest {

    @Test
    fun everyPersonality_playsOnlyLegalMovesAndKeepsInvariants() {
        val random = Random(42)
        val personalities = BotPersonality.entries
        for (pack in SimPacks.all) {
            repeat(360) { game ->
                val a = personalities[game % personalities.size]
                val b = personalities[(game / personalities.size) % personalities.size]
                val result = BotSimulation.playOnce(a, b, pack, random, firstOpener = if (game % 2 == 0) Seat.A else Seat.B)

                assertEquals(0, result.rejectedActions, "$a vs $b on ${pack.theme} made an illegal move")
                assertEquals(0, result.timeouts, "$a vs $b on ${pack.theme} let the clock run out")
                val seats = result.final.seats.values
                assertEquals(GameRules.POOL_SIZE, seats.sumOf { it.cards.size } + result.discarded)
                seats.forEach { seat ->
                    assertTrue(seat.budget >= 0)
                    assertEquals(GameRules.STARTING_BUDGET - seat.budget, seat.cards.sumOf { it.price })
                }
                if (pack.scoring == Scoring.RATINGS) assertEquals(0, result.discarded)
            }
        }
    }

    @Test
    fun firstOpener_hasNoBigAdvantage() {
        for (pack in SimPacks.all) {
            val random = Random(7)
            var openerPoints = 0.0
            val games = 1_000
            repeat(games) {
                val result = BotSimulation.playOnce(
                    BotPersonality.entries.random(random),
                    BotPersonality.entries.random(random),
                    pack,
                    random,
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
            assertTrue(rate in 0.42..0.58, "first opener won $rate on ${pack.theme}")
        }
    }

    @Test
    fun difficultyTiers_areOrderedOnEveryPack() {
        for (pack in SimPacks.all) {
            val average = BotSimulation.tournament(pack, gamesPerPair = 120)
            fun tier(d: BotDifficulty) = BotPersonality.entries.filter { it.difficultyFor(pack.scoring) == d }.map { average.getValue(it) }.average()
            val easy = tier(BotDifficulty.EASY)
            val medium = tier(BotDifficulty.MEDIUM)
            val hard = tier(BotDifficulty.HARD)
            assertTrue(easy < medium && medium < hard, "${pack.theme}: easy $easy, medium $medium, hard $hard")
        }
    }

    @Test
    fun scout_beatsCasualFanMostOfTheTime() {
        for (pack in SimPacks.all) {
            val rate = BotSimulation.winRate(BotPersonality.SCOUT, BotPersonality.CASUAL_FAN, games = 300, seed = 1, pack = pack)
            assertTrue(rate > 0.7, "Scout won only $rate against Casual fan on ${pack.theme}")
        }
    }

    @Test
    fun quickMatch_canMeetEveryPersonality() {
        val random = Random(3)
        val seen = (1..500).map { BotPersonality.randomForQuickMatch(random) }.toSet()
        assertEquals(BotPersonality.entries.toSet(), seen)
    }
}
