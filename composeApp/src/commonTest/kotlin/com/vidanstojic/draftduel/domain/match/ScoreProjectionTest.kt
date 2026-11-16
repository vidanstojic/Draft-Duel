package com.vidanstojic.draftduel.domain.match

import com.vidanstojic.draftduel.domain.bot.BotPersonality
import com.vidanstojic.draftduel.domain.bot.BotSimulation
import com.vidanstojic.draftduel.domain.bot.SimPacks
import com.vidanstojic.draftduel.domain.model.Theme
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The final score reads like the sport, and never changes who won. */
class ScoreProjectionTest {

    private fun matches(theme: Theme, count: Int, seed: Int): List<MatchState> {
        val random = Random(seed)
        val pack = SimPacks.football.copy(theme = theme)
        return List(count) {
            val a = BotPersonality.entries.random(random)
            val b = BotPersonality.entries.random(random)
            BotSimulation.playOnce(a, b, pack, random, Seat.A, MatchRules.DEFAULT).final
        }
    }

    private fun MatchState.winnerByTotals(): Int = seat(Seat.A).total.compareTo(seat(Seat.B).total)

    @Test
    fun football_isAScorelineWithTheSameWinner() {
        for (match in matches(Theme.FOOTBALL, 300, seed = 1)) {
            val score = ScoreProjection.of(match)
            assertEquals(ScoreUnit.GOALS, score.unit)
            assertEquals(match.winnerByTotals(), score.a.compareTo(score.b), "winner changed: $score")
            assertTrue(score.a in 0..8 && score.b in 0..8, "unrealistic $score")
        }
    }

    @Test
    fun basketball_isAScorelineWithTheSameWinner() {
        for (match in matches(Theme.BASKETBALL, 300, seed = 2)) {
            val score = ScoreProjection.of(match)
            assertEquals(ScoreUnit.POINTS, score.unit)
            assertEquals(match.winnerByTotals(), score.a.compareTo(score.b), "winner changed: $score")
            assertTrue(minOf(score.a, score.b) in 68..128, "unrealistic $score")
            assertTrue(score.margin <= 42, "unrealistic $score")
        }
    }

    @Test
    fun otherThemes_keepTheirTotals() {
        for (theme in listOf(Theme.MOVIES, Theme.FOOD)) {
            val match = matches(theme, 1, seed = 3).single()
            assertEquals(FinalScore(match.seat(Seat.A).total, match.seat(Seat.B).total, ScoreUnit.TOTALS), ScoreProjection.of(match))
        }
    }

    @Test
    fun sameMatch_sameScore() {
        val match = matches(Theme.BASKETBALL, 1, seed = 4).single()
        assertEquals(ScoreProjection.of(match), ScoreProjection.of(match.copy()))
    }

    @Test
    fun biggerGap_biggerWin() {
        val random = Random(5)
        fun averageGoalMargin(gap: Double) = (1..400).map { ScoreProjection.football(gap, random).let { (w, l) -> w - l } }.average()
        fun averagePointMargin(gap: Double) = (1..400).map { ScoreProjection.basketball(gap, 80.0, random).let { (w, l) -> w - l } }.average()
        assertTrue(averageGoalMargin(1.0) < averageGoalMargin(5.0))
        assertTrue(averageGoalMargin(5.0) < averageGoalMargin(12.0))
        assertTrue(averagePointMargin(1.0) < averagePointMargin(5.0))
        assertTrue(averagePointMargin(5.0) < averagePointMargin(12.0))
        assertEquals(1.0, averageGoalMargin(0.5))
    }

    @Test
    fun equalTotals_isADraw() {
        val (w, l) = ScoreProjection.football(0.0, Random(6))
        assertEquals(w, l)
        val (pw, pl) = ScoreProjection.basketball(0.0, 80.0, Random(6))
        assertEquals(pw, pl)
    }
}
