package com.vidanstojic.draftduel.ui.screens.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidanstojic.draftduel.domain.bot.BotFactory
import com.vidanstojic.draftduel.domain.bot.BotPersonality
import com.vidanstojic.draftduel.domain.bot.PoolStats
import com.vidanstojic.draftduel.domain.match.MatchAction
import com.vidanstojic.draftduel.domain.match.MatchEngine
import com.vidanstojic.draftduel.domain.match.Seat
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.MatchSetup
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Theme
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
        theme = Theme.FOOTBALL,
        poolSource = PoolSource.Random,
        you = profileRepository.currentPlayer(),
    )
    private val opponent = setup.opponent ?: profileRepository.quickMatchOpponent()
    private val theme = if (packRepository.pack(setup.theme) != null) setup.theme else Theme.FOOTBALL
    private val rules = setup.copy(theme = theme).effectiveRules
    private val pool = requireNotNull(packRepository.drawPool(theme, setup.poolSource, rules.cards)) { "No pack for $theme" }

    /** Hidden in quick matches, chosen by the player in "Play vs bot". Kept for the rematch. */
    private val botPersonality: BotPersonality = setup.bot ?: BotPersonality.randomForQuickMatch(random)

    private val driver = MatchDriver(
        scope = viewModelScope,
        initial = MatchEngine.start(
            theme = theme,
            pool = pool.cards,
            goal = pool.goal?.name,
            rules = rules,
            nameA = setup.you.nickname,
            nameB = opponent.nickname,
            firstOpener = if (random.nextBoolean()) Seat.A else Seat.B,
        ),
        bot = BotFactory.create(
            personality = botPersonality,
            stats = PoolStats.of(pool.drawnFrom, theme.scoring),
            random = random,
            rules = rules,
        ),
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
        initialValue = driver.match.value.toUi(viewer = Seat.A, millisLeft = driver.turnMillis),
    )

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    /**
     * Only a bot the player chose may wait. "Find opponent" stands in for a real person (and will
     * be one online), so the clock keeps running there, as it will in an online match.
     */
    val canPause: Boolean = setup.mode == GameMode.VS_BOT

    val paused: StateFlow<Boolean> = driver.pausedByPlayer

    init {
        driver.start()
    }

    fun onRaise(step: Int) = driver.dispatch(MatchAction.Raise(Seat.A, step))

    /** Pass, or for an opener in five-slot matches a paid hand-over (see [MatchEngine.passAction]). */
    fun onPass() = driver.dispatch(MatchEngine.passAction(driver.match.value, Seat.A))

    fun onAssign(toSelf: Boolean) = driver.dispatch(MatchAction.Assign(chooser = Seat.A, to = if (toSelf) Seat.A else Seat.B))

    fun pause() {
        if (canPause) driver.setPaused(PauseReason.PLAYER, true)
    }

    fun resume() = driver.setPaused(PauseReason.PLAYER, false)
}
