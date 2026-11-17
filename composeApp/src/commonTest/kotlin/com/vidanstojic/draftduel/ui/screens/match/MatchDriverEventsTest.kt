package com.vidanstojic.draftduel.ui.screens.match

import com.vidanstojic.draftduel.domain.bot.SimPacks
import com.vidanstojic.draftduel.domain.match.MatchAction
import com.vidanstojic.draftduel.domain.match.MatchEngine
import com.vidanstojic.draftduel.domain.match.MatchRules
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.model.Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals

/** The phone buzzes when the turn comes to a player, so the driver must say whose turn starts. */
class MatchDriverEventsTest {

    @Test
    fun turnStarted_followsTheTurn() = runTest {
        // The driver runs on a real dispatcher, so the test waits in real time, not virtual time.
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val events = Channel<MatchEvent>(Channel.UNLIMITED)
        suspend fun wait() = withContext(Dispatchers.Default) { delay(150) }
        val driver = MatchDriver(
            scope = scope,
            initial = MatchEngine.start(Theme.FOOTBALL, SimPacks.football.cards.take(10), "A", "B", Seat.A, rules = MatchRules.DEFAULT),
            onFinished = {},
            onEvent = { events.trySend(it) },
        )
        driver.start()
        wait()
        driver.dispatch(MatchAction.Raise(Seat.A, 1))
        wait()
        driver.dispatch(MatchAction.Raise(Seat.B, 2))
        wait()
        scope.cancel()
        events.close()
        val turns = buildList { for (event in events) if (event is MatchEvent.TurnStarted) add(event.seat) }
        assertEquals(listOf(Seat.A, Seat.B, Seat.A), turns)
    }
}
