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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs a local match: the 10 s turn clock, the pause after each award and the bot's moves.
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
    private var paused = false

    fun start() = onMatchChanged()

    fun dispatch(action: MatchAction) {
        val current = _match.value
        val next = MatchEngine.reduce(current, action)
        if (next !== current) {
            _match.value = next
            onMatchChanged()
        }
    }

    /** Stops the clock, e.g. while the phone is handed to the other player. */
    fun setPaused(value: Boolean) {
        paused = value
    }

    private fun onMatchChanged() {
        job?.cancel()
        val state = _match.value
        job = scope.launch {
            when (state.phase) {
                is MatchPhase.Awarded, MatchPhase.Discarded -> {
                    delay(GameRules.AWARD_PAUSE_MILLIS)
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
                delay(bot.thinkTimeMillis())
                // The strongest bot builds a lookahead table on its first move; keep that off the UI thread.
                val snapshot = _match.value
                val action = withContext(Dispatchers.Default) { bot.decide(snapshot, botSeat) }
                if (_match.value === snapshot) action?.let(::dispatch)
            }
        }
        var left = turnMillis
        while (left > 0) {
            delay(TICK_MILLIS)
            if (!paused) {
                left -= TICK_MILLIS
                _millisLeft.value = left
            }
        }
        dispatch(MatchAction.Timeout)
    }

    private companion object {
        const val TICK_MILLIS = 100L
    }
}
