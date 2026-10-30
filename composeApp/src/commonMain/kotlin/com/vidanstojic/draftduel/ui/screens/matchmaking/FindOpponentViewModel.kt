package com.vidanstojic.draftduel.ui.screens.matchmaking

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidanstojic.draftduel.domain.bot.BotPersonality
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.MatchSetup
import com.vidanstojic.draftduel.domain.model.PlayerProfile
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.domain.repository.MatchSessionRepository
import com.vidanstojic.draftduel.domain.repository.ProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class FindOpponentUiState(
    val theme: Theme,
    val you: PlayerProfile,
    val elapsedSeconds: Int = 0,
    val opponent: PlayerProfile? = null,
    val startsIn: Int? = null,
    val ready: Boolean = false,
)

/**
 * "Find opponent". In v1 the opponent is always the on-device bot with a player-like identity;
 * v2 swaps the repository for real matchmaking without touching this screen.
 */
class FindOpponentViewModel(
    private val profileRepository: ProfileRepository,
    private val sessionRepository: MatchSessionRepository,
) : ViewModel() {

    private val setup: MatchSetup = sessionRepository.setup ?: MatchSetup(
        mode = GameMode.QUICK_MATCH,
        theme = Theme.FOOTBALL,
        poolSource = PoolSource.Random,
        you = profileRepository.currentPlayer(),
    )

    private val _state = MutableStateFlow(FindOpponentUiState(theme = setup.theme, you = setup.you))
    val state: StateFlow<FindOpponentUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { search() }
    }

    private suspend fun search() {
        repeat(SEARCH_SECONDS) {
            delay(1_000)
            _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
        }
        val opponent = profileRepository.quickMatchOpponent()
        sessionRepository.setup = setup.copy(opponent = opponent, bot = BotPersonality.randomForQuickMatch())
        _state.update { it.copy(opponent = opponent) }
        for (seconds in COUNTDOWN_SECONDS downTo 1) {
            _state.update { it.copy(startsIn = seconds) }
            delay(1_000)
        }
        _state.update { it.copy(startsIn = 0, ready = true) }
    }

    private companion object {
        const val SEARCH_SECONDS = 3
        const val COUNTDOWN_SECONDS = 3
    }
}
