package com.vidanstojic.draftduel.ui.screens.match

import com.vidanstojic.draftduel.domain.bot.MatchBot
import com.vidanstojic.draftduel.domain.match.GameRules
import com.vidanstojic.draftduel.domain.match.MatchAction
import com.vidanstojic.draftduel.domain.match.MatchEngine
import com.vidanstojic.draftduel.domain.match.MatchPhase
import com.vidanstojic.draftduel.domain.match.MatchState
import com.vidanstojic.draftduel.domain.match.Seat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs a local match: the 10 s turn clock, the pause after each award, the bot's moves and the
 * pause menu.
 * Used by both the bot match and the same-device match; an online match will replace it with
 * server events (spec §10) while the screens stay the same.
 */
class MatchDriver(
    private val scope: CoroutineScope,
    initial: MatchState,
    private val bot: MatchBot? = null,
    private val botSeat: Seat? = null,
    private val onFinished: (MatchState) -> Unit,
    /** What just happened at the table, for sounds and vibration. */
    private val onEvent: (MatchEvent) -> Unit = {},
) {
    private val _match = MutableStateFlow(initial)
    val match: StateFlow<MatchState> = _match.asStateFlow()

    /** Length of one turn, from the match rules. */
    val turnMillis: Long = initial.rules.turnSeconds * 1_000L

    private val _millisLeft = MutableStateFlow(turnMillis)
    val millisLeft: StateFlow<Long> = _millisLeft.asStateFlow()

    private var job: Job? = null

    /** Why the match is on hold; it runs again only when nothing is left here. */
    private val pauseReasons = MutableStateFlow<Set<PauseReason>>(emptySet())

    private val _pausedByPlayer = MutableStateFlow(false)

    /** True while a player paused the match (the pause menu is on screen). */
    val pausedByPlayer: StateFlow<Boolean> = _pausedByPlayer.asStateFlow()

    private val isPaused: Boolean get() = pauseReasons.value.isNotEmpty()

    fun start() = onMatchChanged()

    fun dispatch(action: MatchAction) {
        // While the pause menu is open nothing on the table may change, not even a late tap.
        if (PauseReason.PLAYER in pauseReasons.value) return
        val current = _match.value
        val next = MatchEngine.reduce(current, action)
        if (next !== current) {
            _match.value = next
            eventsFor(current, action, next).forEach(onEvent)
            onMatchChanged()
        }
    }

    private fun eventsFor(before: MatchState, action: MatchAction, after: MatchState): List<MatchEvent> {
        val first = when (action) {
            is MatchAction.Raise -> MatchEvent.Raised(action.seat)
            is MatchAction.Pass -> MatchEvent.Passed(action.seat)
            is MatchAction.HandOver -> MatchEvent.Passed(action.seat)
            is MatchAction.Assign -> null
            MatchAction.Timeout -> seatToAct(before)?.let { MatchEvent.TimedOut(it) }
        }
        val awarded = (after.phase as? MatchPhase.Awarded)?.let { MatchEvent.CardAwarded(it.winner) }
        return listOfNotNull(first, awarded)
    }

    private fun seatToAct(state: MatchState): Seat? = when (val phase = state.phase) {
        is MatchPhase.Bidding -> phase.toAct
        is MatchPhase.Choosing -> phase.chooser
        else -> null
    }

    /**
     * Holds or releases the match for one [reason]. While anything holds it, the turn clock, the
     * pause after an award and the bot all wait; nothing is lost, the match continues where it was.
     */
    fun setPaused(reason: PauseReason, value: Boolean) {
        pauseReasons.value = if (value) pauseReasons.value + reason else pauseReasons.value - reason
        _pausedByPlayer.value = PauseReason.PLAYER in pauseReasons.value
    }

    private fun onMatchChanged() {
        job?.cancel()
        val state = _match.value
        job = scope.launch {
            when (state.phase) {
                is MatchPhase.Awarded, MatchPhase.Discarded -> {
                    pausableDelay(GameRules.AWARD_PAUSE_MILLIS)
                    val next = MatchEngine.advance(_match.value)
                    _match.value = next
                    // Cards that go to the other player for free once one is full are awarded here.
                    (next.phase as? MatchPhase.Awarded)?.let { onEvent(MatchEvent.CardAwarded(it.winner)) }
                    onMatchChanged()
                }
                MatchPhase.Finished -> onFinished(state)
                is MatchPhase.Bidding, is MatchPhase.Choosing -> runTurn(state)
            }
        }
    }

    private suspend fun runTurn(state: MatchState) = coroutineScope {
        _millisLeft.value = turnMillis
        val seatToAct = seatToAct(state)
        seatToAct?.let { onEvent(MatchEvent.TurnStarted(it)) }
        if (bot != null && botSeat != null && seatToAct == botSeat) {
            launch {
                pausableDelay(bot.thinkTimeMillis())
                // The strongest bot builds a lookahead table on its first move; keep that off the UI thread.
                val snapshot = _match.value
                val action = withContext(Dispatchers.Default) { bot.decide(snapshot, botSeat) }
                // A pause that started while the bot was thinking holds its move until the match runs again.
                pauseReasons.first { it.isEmpty() }
                if (_match.value === snapshot) action?.let(::dispatch)
            }
        }
        var left = turnMillis
        while (left > 0) {
            delay(TICK_MILLIS)
            if (!isPaused) {
                left -= TICK_MILLIS
                _millisLeft.value = left
                if (seatToAct != null && left in 1..WARN_MILLIS && left % 1_000 == 0L) {
                    onEvent(MatchEvent.LastSeconds(seatToAct, (left / 1_000).toInt()))
                }
            }
        }
        dispatch(MatchAction.Timeout)
    }

    /** Like [delay], but time spent paused does not count. */
    private suspend fun pausableDelay(millis: Long) {
        var left = millis
        while (left > 0) {
            val step = minOf(TICK_MILLIS, left)
            delay(step)
            if (!isPaused) left -= step
        }
    }

    private companion object {
        const val TICK_MILLIS = 100L

        /** The last seconds of a turn are counted out loud: 3, 2, 1. */
        const val WARN_MILLIS = 3_000L
    }
}

/** Things at the table worth a sound or a buzz. */
sealed interface MatchEvent {
    /** [seat] is now to act: to bid, pass, hand over or choose who gets a free card. */
    data class TurnStarted(val seat: Seat) : MatchEvent

    data class Raised(val seat: Seat) : MatchEvent

    /** A pass, a skip or a paid hand-over. */
    data class Passed(val seat: Seat) : MatchEvent

    data class CardAwarded(val winner: Seat) : MatchEvent

    /** [secondsLeft] (3, 2, 1) of the turn of [seat]. */
    data class LastSeconds(val seat: Seat, val secondsLeft: Int) : MatchEvent

    /** The clock ran out on [seat]. */
    data class TimedOut(val seat: Seat) : MatchEvent
}

/** What can hold a local match. */
enum class PauseReason {
    /** Pass-the-phone: the next player has not taken the phone yet. */
    HANDOFF,

    /** A player opened the pause menu, or the app went to the background. */
    PLAYER,
}
