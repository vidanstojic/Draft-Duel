package com.vidanstojic.draftduel.ui.screens.local

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidanstojic.draftduel.domain.match.MatchAction
import com.vidanstojic.draftduel.domain.match.MatchEngine
import com.vidanstojic.draftduel.domain.match.MatchPhase
import com.vidanstojic.draftduel.domain.match.MatchState
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.model.GameCard
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.LocalLayout
import com.vidanstojic.draftduel.domain.model.MatchSetup
import com.vidanstojic.draftduel.domain.model.PlayerProfile
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.domain.repository.MatchSessionRepository
import com.vidanstojic.draftduel.domain.repository.PackRepository
import com.vidanstojic.draftduel.ui.screens.match.MatchDriver
import com.vidanstojic.draftduel.ui.screens.match.MatchUiState
import com.vidanstojic.draftduel.ui.screens.match.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Screen shown while the phone changes hands; the clock is paused. */
@Immutable
data class HandoffUi(
    val nextPlayer: String,
    val card: GameCard?,
    val price: Int,
    val leaderName: String?,
)

@Immutable
data class LocalMatchUiState(
    val layout: LocalLayout,
    /** Player 1, bottom of the split screen. */
    val bottom: MatchUiState,
    /** Player 2, top of the split screen (drawn rotated 180°). */
    val top: MatchUiState,
    /** Pass-the-phone: whose view is on screen. */
    val activeSeat: Seat,
    val handoff: HandoffUi?,
) {
    val active: MatchUiState get() = if (activeSeat == Seat.A) bottom else top
}

/**
 * Two people on one phone. In the split layout both see the same table, so ratings stay hidden for
 * both until the result. Passing the phone lets each player see their own cards on their turn.
 */
class LocalMatchViewModel(
    packRepository: PackRepository,
    private val sessionRepository: MatchSessionRepository,
    random: Random = Random.Default,
) : ViewModel() {

    private val setup: MatchSetup = sessionRepository.setup ?: MatchSetup(
        mode = GameMode.SAME_DEVICE,
        theme = Theme.FOOTBALL,
        poolSource = PoolSource.Random,
        you = PlayerProfile("Player 1", "", 0),
        opponent = PlayerProfile("Player 2", "", 0),
    )
    private val layout = setup.localLayout
    private val revealOwn = layout == LocalLayout.PASS_DEVICE
    private val theme = if (packRepository.pack(setup.theme) != null) setup.theme else Theme.FOOTBALL
    private val pool = requireNotNull(packRepository.drawPool(theme, setup.poolSource)) { "No pack for $theme" }

    private val driver = MatchDriver(
        scope = viewModelScope,
        initial = MatchEngine.start(
            theme = theme,
            pool = pool.cards,
            goal = pool.goal?.name,
            ruleSet = setup.ruleSet,
            nameA = setup.you.nickname,
            nameB = setup.opponent?.nickname ?: "Player 2",
            firstOpener = if (random.nextBoolean()) Seat.A else Seat.B,
        ),
        onFinished = { finishedMatch ->
            sessionRepository.lastFinishedMatch = finishedMatch
            _finished.value = true
        },
    )

    /**
     * Seat whose view is confirmed on screen in the pass-the-phone layout. It starts on the
     * non-opener, so the match opens with a hand-off to the first player.
     */
    private val confirmedSeat = MutableStateFlow((seatToAct(driver.match.value) ?: Seat.A).other)

    val state: StateFlow<LocalMatchUiState> = combine(driver.match, driver.millisLeft, confirmedSeat) { match, millis, confirmed ->
        project(match, millis, confirmed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = project(driver.match.value, MatchDriver.TURN_MILLIS, confirmedSeat.value),
    )

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    init {
        if (layout == LocalLayout.PASS_DEVICE) {
            // Pause the clock whenever the turn moves to the player who does not hold the phone,
            // whether after a move, a timeout or a new card.
            viewModelScope.launch {
                driver.match.collect { match ->
                    if (needsHandoff(match, confirmedSeat.value)) driver.setPaused(true)
                }
            }
        }
        driver.start()
    }

    fun onRaise(seat: Seat, step: Int) = act(MatchAction.Raise(seat, step))

    fun onPass(seat: Seat) = act(MatchEngine.passAction(driver.match.value, seat))

    fun onAssign(chooser: Seat, toSelf: Boolean) = act(MatchAction.Assign(chooser, if (toSelf) chooser else chooser.other))

    /** The next player has the phone. */
    fun onHandoffConfirmed() {
        seatToAct(driver.match.value)?.let { confirmedSeat.value = it }
        driver.setPaused(false)
    }

    private fun act(action: MatchAction) = driver.dispatch(action)

    private fun project(match: MatchState, millis: Long, confirmed: Seat): LocalMatchUiState {
        val handoff = if (layout == LocalLayout.PASS_DEVICE && needsHandoff(match, confirmed)) {
            val next = seatToAct(match) ?: confirmed
            val bidding = match.phase as? MatchPhase.Bidding
            HandoffUi(
                nextPlayer = match.seat(next).name,
                card = match.currentCard,
                price = bidding?.price ?: 0,
                leaderName = bidding?.leader?.let { match.seat(it).name },
            )
        } else {
            null
        }
        return LocalMatchUiState(
            layout = layout,
            bottom = match.toUi(viewer = Seat.A, millisLeft = millis, revealOwnRatings = revealOwn),
            top = match.toUi(viewer = Seat.B, millisLeft = millis, revealOwnRatings = revealOwn),
            activeSeat = confirmed,
            handoff = handoff,
        )
    }

    private fun needsHandoff(match: MatchState, confirmed: Seat): Boolean {
        val next = seatToAct(match) ?: return false
        return next != confirmed
    }

    private fun seatToAct(match: MatchState): Seat? = when (val phase = match.phase) {
        is MatchPhase.Bidding -> phase.toAct
        is MatchPhase.Choosing -> phase.chooser
        else -> null
    }
}
