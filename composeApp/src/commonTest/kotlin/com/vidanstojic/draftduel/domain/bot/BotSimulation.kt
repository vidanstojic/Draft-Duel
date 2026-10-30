package com.vidanstojic.draftduel.domain.bot

import com.vidanstojic.draftduel.domain.match.MatchAction
import com.vidanstojic.draftduel.domain.match.MatchEngine
import com.vidanstojic.draftduel.domain.match.MatchOutcome
import com.vidanstojic.draftduel.domain.match.MatchPhase
import com.vidanstojic.draftduel.domain.match.MatchState
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.model.AthleteCard
import com.vidanstojic.draftduel.domain.model.AvatarColorway
import com.vidanstojic.draftduel.domain.model.AvatarSpec
import com.vidanstojic.draftduel.domain.model.Theme
import kotlin.random.Random

/** Ratings and positions of the bundled football pack (v5 model, 50 athletes), without names. */
internal object FootballPackFixture {

    private val entries = listOf(
        99 to "FWD", 96 to "FWD", 94 to "FWD", 92 to "MID", 89 to "MID", 89 to "GK", 89 to "DEF", 89 to "MID",
        89 to "FWD", 89 to "DEF", 88 to "FWD", 88 to "MID", 88 to "DEF", 88 to "MID", 88 to "DEF", 88 to "FWD",
        88 to "FWD", 87 to "DEF", 86 to "MID", 86 to "MID", 79 to "DEF", 79 to "FWD", 79 to "FWD", 79 to "FWD",
        79 to "FWD", 78 to "GK", 78 to "MID", 78 to "DEF", 77 to "MID", 77 to "DEF", 76 to "MID", 76 to "MID",
        76 to "MID", 76 to "DEF", 76 to "GK", 76 to "MID", 75 to "DEF", 75 to "FWD", 75 to "MID", 75 to "DEF",
        74 to "MID", 74 to "MID", 74 to "GK", 74 to "DEF", 74 to "MID", 74 to "GK", 74 to "DEF", 73 to "FWD",
        73 to "FWD", 71 to "DEF",
    )

    val cards: List<AthleteCard> = entries.mapIndexed { i, (rating, position) ->
        AthleteCard(
            id = "fb-$i",
            name = "Athlete $i",
            season = "2025/26",
            country = "England",
            position = position,
            rating = rating,
            avatar = AvatarSpec(i % 99 + 1, "A$i", AvatarColorway.entries[i % AvatarColorway.entries.size]),
            categoryIds = emptySet(),
        )
    }

    val stats = PoolStats(cards.map { it.rating })
}

internal data class SimulatedMatch(
    val final: MatchState,
    val outcome: MatchOutcome,
    /** Actions a bot returned that the engine rejected (should always be 0). */
    val rejectedActions: Int,
    val timeouts: Int,
)

/** Plays bot-vs-bot matches without clocks or coroutines (spec §17 simulation). */
internal object BotSimulation {

    fun play(
        botA: MatchBot,
        botB: MatchBot,
        pool: List<AthleteCard>,
        firstOpener: Seat,
    ): SimulatedMatch {
        var state = MatchEngine.start(Theme.FOOTBALL, pool, "A", "B", firstOpener)
        var rejected = 0
        var timeouts = 0
        var guard = 0
        while (!state.isFinished) {
            check(guard++ < 10_000) { "Match did not finish" }
            if (state.phase is MatchPhase.Awarded) {
                state = MatchEngine.advance(state)
                continue
            }
            val seat = when (val phase = state.phase) {
                is MatchPhase.Bidding -> phase.toAct
                is MatchPhase.Choosing -> phase.chooser
                else -> error("Unexpected phase $phase")
            }
            val bot = if (seat == Seat.A) botA else botB
            val action = bot.decide(state, seat)
            val next = action?.let { MatchEngine.reduce(state, it) }
            state = if (next == null || next === state) {
                if (action != null) rejected++
                timeouts++
                MatchEngine.reduce(state, MatchAction.Timeout)
            } else {
                next
            }
        }
        return SimulatedMatch(state, MatchEngine.outcome(state), rejected, timeouts)
    }

    /** Win rate of [a] against [b] over [games], seats and first opener alternating. Draws count half. */
    fun winRate(a: BotPersonality, b: BotPersonality, games: Int, seed: Int): Double {
        val random = Random(seed)
        var points = 0.0
        repeat(games) { game ->
            val pool = FootballPackFixture.cards.shuffled(random).take(10)
            val aSitsA = game % 2 == 0
            val botA = BotFactory.create(if (aSitsA) a else b, FootballPackFixture.stats, Random(random.nextInt()))
            val botB = BotFactory.create(if (aSitsA) b else a, FootballPackFixture.stats, Random(random.nextInt()))
            val opener = if ((game / 2) % 2 == 0) Seat.A else Seat.B
            val result = play(botA, botB, pool, opener)
            val aWon = if (aSitsA) MatchOutcome.A_WINS else MatchOutcome.B_WINS
            points += when (result.outcome) {
                aWon -> 1.0
                MatchOutcome.DRAW -> 0.5
                else -> 0.0
            }
        }
        return points / games
    }
}
