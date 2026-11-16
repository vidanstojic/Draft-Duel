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
            onMatchChanged()
        }
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
                    _match.value = MatchEngine.advance(_match.value)
                    onMatchChanged()
                }
                MatchPhase.Finished -> onFinished(state)
                is MatchPhase.Bidding, is MatchPhase.Choosing -> runTurn(state)
            }
        }
    }

    private suspend fun runTurn(state: MatchState) = coroutineScope {
        _millisLeft.value = turnMillis
        val seatToAct = when (val phase = state.phase) {
            is MatchPhase.Bidding -> phase.toAct
            is MatchPhase.Choosing -> phase.chooser
            else -> null
        }
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
    }
}

/** What can hold a local match. */
enum class PauseReason {
    /** Pass-the-phone: the next player has not taken the phone yet. */
    HANDOFF,

    /** A player opened the pause menu, or the app went to the background. */
    PLAYER,
}
