package com.vidanstojic.draftduel.ui.screens.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidanstojic.draftduel.domain.match.BotStrategy
import com.vidanstojic.draftduel.domain.match.MatchAction
import com.vidanstojic.draftduel.domain.match.MatchEngine
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.MatchSetup
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Sport
import com.vidanstojic.draftduel.domain.repository.MatchSessionRepository
import com.vidanstojic.draftduel.domain.repository.PackRepository
import com.vidanstojic.draftduel.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.random.Random

/** Match against the on-device bot. The local user always sits in [Seat.A]. */
class MatchViewModel(
    packRepository: PackRepository,
    profileRepository: ProfileRepository,
    private val sessionRepository: MatchSessionRepository,
    random: Random = Random.Default,
) : ViewModel() {

    private val setup: MatchSetup = sessionRepository.setup ?: MatchSetup(
        mode = GameMode.QUICK_MATCH,
        sport = Sport.FOOTBALL,
        poolSource = PoolSource.Random,
        you = profileRepository.currentPlayer(),
    )
    private val opponent = setup.opponent ?: profileRepository.quickMatchOpponent()
    private val sport = if (packRepository.pack(setup.sport) != null) setup.sport else Sport.FOOTBALL
    private val pack = requireNotNull(packRepository.pack(sport)) { "No pack for $sport" }

    private val driver = MatchDriver(
        scope = viewModelScope,
        initial = MatchEngine.start(
            sport = sport,
            pool = packRepository.drawPool(sport, setup.poolSource),
            nameA = setup.you.nickname,
            nameB = opponent.nickname,
            firstOpener = if (random.nextBoolean()) Seat.A else Seat.B,
        ),
        bot = BotStrategy(averageRating = pack.averageRating, random = random),
        botSeat = Seat.B,
        onFinished = { finishedMatch ->
            sessionRepository.lastFinishedMatch = finishedMatch
            _finished.value = true
        },
    )

    val state: StateFlow<MatchUiState> = combine(driver.match, driver.millisLeft) { match, millis ->
        match.toUi(viewer = Seat.A, millisLeft = millis)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = driver.match.value.toUi(viewer = Seat.A, millisLeft = MatchDriver.TURN_MILLIS),
    )

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    init {
        driver.start()
    }

    fun onRaise(step: Int) = driver.dispatch(MatchAction.Raise(Seat.A, step))

    fun onPass() = driver.dispatch(MatchAction.Pass(Seat.A))

    fun onAssign(toSelf: Boolean) = driver.dispatch(MatchAction.Assign(chooser = Seat.A, to = if (toSelf) Seat.A else Seat.B))
}
