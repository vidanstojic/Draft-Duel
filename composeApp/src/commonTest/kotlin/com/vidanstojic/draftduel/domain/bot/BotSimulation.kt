package com.vidanstojic.draftduel.domain.bot

import com.vidanstojic.draftduel.domain.match.MatchAction
import com.vidanstojic.draftduel.domain.match.MatchEngine
import com.vidanstojic.draftduel.domain.match.MatchOutcome
import com.vidanstojic.draftduel.domain.match.MatchPhase
import com.vidanstojic.draftduel.domain.match.MatchState
import com.vidanstojic.draftduel.domain.match.MatchRules
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.model.AvatarColorway
import com.vidanstojic.draftduel.domain.model.AvatarSpec
import com.vidanstojic.draftduel.domain.model.GameCard
import com.vidanstojic.draftduel.domain.model.MatchPool
import com.vidanstojic.draftduel.domain.model.PackCategory
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.domain.model.ThemePack
import kotlin.random.Random

/** Value-only copies of the bundled packs (no names), so simulations run without resources. */
internal object SimPacks {

    /** Football, v5 model: 50 athletes. */
    val football: ThemePack = ratingPack(
        Theme.FOOTBALL,
        listOf(
            99 to "FWD", 96 to "FWD", 94 to "FWD", 92 to "MID", 89 to "MID", 89 to "GK", 89 to "DEF", 89 to "MID",
            89 to "FWD", 89 to "DEF", 88 to "FWD", 88 to "MID", 88 to "DEF", 88 to "MID", 88 to "DEF", 88 to "FWD",
            88 to "FWD", 87 to "DEF", 86 to "MID", 86 to "MID", 79 to "DEF", 79 to "FWD", 79 to "FWD", 79 to "FWD",
            79 to "FWD", 78 to "GK", 78 to "MID", 78 to "DEF", 77 to "MID", 77 to "DEF", 76 to "MID", 76 to "MID",
            76 to "MID", 76 to "DEF", 76 to "GK", 76 to "MID", 75 to "DEF", 75 to "FWD", 75 to "MID", 75 to "DEF",
            74 to "MID", 74 to "MID", 74 to "GK", 74 to "DEF", 74 to "MID", 74 to "GK", 74 to "DEF", 73 to "FWD",
            73 to "FWD", 71 to "DEF",
        ),
    )

    /** Movies v1: 60 films, 6 stars / 18 strong / 36 solid. */
    val movies: ThemePack = ratingPack(
        Theme.MOVIES,
        listOf(
            99, 97, 96, 95, 94, 93, 89, 89, 88, 88, 87, 87, 86, 86, 86, 85, 85, 84, 84, 83, 83, 82, 81, 80, 79, 79,
            79, 78, 78, 77, 77, 76, 76, 75, 75, 74, 74, 74, 73, 73, 72, 71, 71, 70, 70, 69, 68, 67, 67, 66, 65, 65,
            64, 64, 63, 63, 62, 62, 61, 60,
        ).map { it to "DRAMA" },
    )

    /** Food v1, three of its dishes: fitting ingredients sum to 100, wrong ones are negative. */
    val food: ThemePack = recipePack(
        mapOf(
            "pizza" to listOf(30, 25, 25, 10, 5, 5, -15, -5, -10, -20, -10, -10),
            "burger" to listOf(20, 30, 15, 10, 10, 10, 5, -15, -5, -15, -10, -20),
            "carbonara" to listOf(30, 25, 20, 15, 10, -10, -5, -10, -20, -10, -10),
        ),
    )

    val all: List<ThemePack> = listOf(football, movies, food)

    private fun ratingPack(theme: Theme, entries: List<Pair<Int, String>>) = ThemePack(
        id = "sim-${theme.name.lowercase()}",
        theme = theme,
        name = theme.name,
        version = 1,
        categories = emptyList(),
        cards = entries.mapIndexed { i, (rating, position) -> card("${theme.name}-$i", rating, position, emptySet()) },
    )

    private fun recipePack(dishes: Map<String, List<Int>>) = ThemePack(
        id = "sim-food",
        theme = Theme.FOOD,
        name = "Food",
        version = 1,
        categories = dishes.keys.map { PackCategory(it, it) },
        cards = dishes.flatMap { (dish, points) -> points.mapIndexed { i, p -> card("$dish-$i", p, "VEG", setOf(dish)) } },
    )

    private fun card(id: String, rating: Int, position: String, categories: Set<String>) = GameCard(
        id = id,
        name = id,
        season = "",
        country = "",
        position = position,
        rating = rating,
        avatar = AvatarSpec(1, "XX", AvatarColorway.entries[id.length % AvatarColorway.entries.size]),
        categoryIds = categories,
    )
}

internal data class SimulatedMatch(
    val final: MatchState,
    val outcome: MatchOutcome,
    /** Actions a bot returned that the engine rejected (should always be 0). */
    val rejectedActions: Int,
    val timeouts: Int,
    val discarded: Int,
    val handOvers: Int,
)

/** Plays bot-vs-bot matches without clocks or coroutines (spec §17 simulation). */
internal object BotSimulation {

    fun play(
        botA: MatchBot,
        botB: MatchBot,
        pack: ThemePack,
        pool: MatchPool,
        firstOpener: Seat,
        rules: MatchRules = MatchRules.CLASSIC,
    ): SimulatedMatch {
        var state = MatchEngine.start(pack.theme, pool.cards, "A", "B", firstOpener, goal = pool.goal?.name, rules = rules)
        var rejected = 0
        var timeouts = 0
        var discarded = 0
        var guard = 0
        while (!state.isFinished) {
            check(guard++ < 10_000) { "Match did not finish" }
            if (state.isSettled) {
                if (state.phase == MatchPhase.Discarded) discarded++
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
        return SimulatedMatch(state, MatchEngine.outcome(state), rejected, timeouts, discarded, state.seats.values.sumOf { it.handOvers })
    }

    /** One match on [pack] with fresh bots; seat A plays [a]. */
    fun playOnce(
        a: BotPersonality,
        b: BotPersonality,
        pack: ThemePack,
        random: Random,
        firstOpener: Seat,
        rules: MatchRules = MatchRules.CLASSIC,
    ): SimulatedMatch {
        val pool = pack.drawPool(PoolSource.Random, rules.cards, random)
        val stats = PoolStats.of(pool.drawnFrom, pack.scoring)
        return play(
            botA = BotFactory.create(a, stats, Random(random.nextInt()), rules),
            botB = BotFactory.create(b, stats, Random(random.nextInt()), rules),
            pack = pack,
            pool = pool,
            firstOpener = firstOpener,
            rules = rules,
        )
    }

    /** Win rate of [a] against [b] over [games] on [pack], seats and first opener alternating. Draws count half. */
    fun winRate(
        a: BotPersonality,
        b: BotPersonality,
        games: Int,
        seed: Int,
        pack: ThemePack = SimPacks.football,
        rules: MatchRules = MatchRules.CLASSIC,
    ): Double {
        val random = Random(seed)
        var points = 0.0
        repeat(games) { game ->
            val aSitsA = game % 2 == 0
            val opener = if ((game / 2) % 2 == 0) Seat.A else Seat.B
            val result = playOnce(if (aSitsA) a else b, if (aSitsA) b else a, pack, random, opener, rules)
            val aWon = if (aSitsA) MatchOutcome.A_WINS else MatchOutcome.B_WINS
            points += when (result.outcome) {
                aWon -> 1.0
                MatchOutcome.DRAW -> 0.5
                else -> 0.0
            }
        }
        return points / games
    }

    /** Average win rate of each personality against all the others on [pack]. */
    fun tournament(pack: ThemePack, gamesPerPair: Int, seed: Int = 0, rules: MatchRules = MatchRules.CLASSIC): Map<BotPersonality, Double> {
        val all = BotPersonality.entries
        return all.associateWith { a ->
            all.filter { it != a }
                .map { b -> winRate(a, b, gamesPerPair, seed = seed + a.ordinal * 31 + b.ordinal, pack = pack, rules = rules) }
                .average()
        }
    }
}
