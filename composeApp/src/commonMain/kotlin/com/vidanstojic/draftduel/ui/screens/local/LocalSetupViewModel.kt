package com.vidanstojic.draftduel.ui.screens.local

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.LocalLayout
import com.vidanstojic.draftduel.domain.model.MatchSetup
import com.vidanstojic.draftduel.domain.model.PlayerProfile
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Sport
import com.vidanstojic.draftduel.domain.repository.MatchSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class LocalSetupUiState(
    val sport: Sport,
    val playerOne: String = "",
    val playerTwo: String = "",
    val layout: LocalLayout = LocalLayout.SPLIT_SCREEN,
) {
    val canStart: Boolean get() = playerOne.isNotBlank() && playerTwo.isNotBlank() && playerOne.trim() != playerTwo.trim()
}

class LocalSetupViewModel(
    private val sessionRepository: MatchSessionRepository,
    defaultPlayerOne: String,
    defaultPlayerTwo: String,
) : ViewModel() {

    private val baseSetup: MatchSetup? = sessionRepository.setup

    private val _state = MutableStateFlow(
        LocalSetupUiState(
            sport = baseSetup?.sport ?: Sport.FOOTBALL,
            playerOne = defaultPlayerOne,
            playerTwo = defaultPlayerTwo,
        ),
    )
    val state: StateFlow<LocalSetupUiState> = _state.asStateFlow()

    fun onPlayerOneChange(value: String) = _state.update { it.copy(playerOne = value.take(MAX_NAME_LENGTH)) }

    fun onPlayerTwoChange(value: String) = _state.update { it.copy(playerTwo = value.take(MAX_NAME_LENGTH)) }

    fun onLayoutSelected(layout: LocalLayout) = _state.update { it.copy(layout = layout) }

    fun confirm() {
        val current = _state.value
        sessionRepository.setup = MatchSetup(
            mode = GameMode.SAME_DEVICE,
            sport = current.sport,
            poolSource = baseSetup?.poolSource ?: PoolSource.Random,
            you = PlayerProfile(nickname = current.playerOne.trim(), country = "", wins = 0),
            opponent = PlayerProfile(nickname = current.playerTwo.trim(), country = "", wins = 0),
            localLayout = current.layout,
        )
    }

    private companion object {
        const val MAX_NAME_LENGTH = 12
    }
}
