package com.vidanstojic.draftduel.ui.screens.invite

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidanstojic.draftduel.domain.bot.BotPersonality
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.MatchSetup
import com.vidanstojic.draftduel.domain.model.PlayerProfile
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Sport
import com.vidanstojic.draftduel.domain.repository.MatchSessionRepository
import com.vidanstojic.draftduel.domain.repository.ProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Immutable
data class InviteUiState(
    val sport: Sport,
    val code: String,
    val link: String,
    val expiresInSeconds: Int,
    val waitedSeconds: Int = 0,
    val joinCode: String = "",
    val copied: Boolean = false,
) {
    /** Spec §7: after 60 s without the friend, offer a match against the bot. */
    val botOffered: Boolean get() = waitedSeconds >= BOT_OFFER_AFTER_SECONDS
    val botOfferInSeconds: Int get() = (BOT_OFFER_AFTER_SECONDS - waitedSeconds).coerceAtLeast(0)
    val canJoin: Boolean get() = joinCode.filter { it.isLetterOrDigit() }.length >= 6

    companion object {
        const val BOT_OFFER_AFTER_SECONDS = 60
        const val LINK_VALID_SECONDS = 10 * 60
    }
}

/** Fake invite: a fixed code and link; joining by code or falling back to the bot starts a local match. */
class InviteViewModel(
    private val profileRepository: ProfileRepository,
    private val sessionRepository: MatchSessionRepository,
) : ViewModel() {

    private val setup: MatchSetup = sessionRepository.setup ?: MatchSetup(
        mode = GameMode.FRIEND,
        sport = Sport.FOOTBALL,
        poolSource = PoolSource.Random,
        you = profileRepository.currentPlayer(),
    )

    private val _state = MutableStateFlow(
        InviteUiState(
            sport = setup.sport,
            code = SAMPLE_CODE,
            link = "draftduel.gg/j/$SAMPLE_CODE",
            expiresInSeconds = InviteUiState.LINK_VALID_SECONDS,
        ),
    )
    val state: StateFlow<InviteUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                _state.update {
                    it.copy(
                        waitedSeconds = it.waitedSeconds + 1,
                        expiresInSeconds = (it.expiresInSeconds - 1).coerceAtLeast(0),
                    )
                }
            }
        }
    }

    fun onJoinCodeChange(value: String) {
        _state.update { it.copy(joinCode = value.uppercase().take(MAX_CODE_LENGTH)) }
    }

    fun onCopied() {
        _state.update { it.copy(copied = true) }
    }

    /** A friend joined (or we joined theirs): start the match against them. */
    fun startWithFriend() {
        sessionRepository.setup = setup.copy(opponent = PlayerProfile(nickname = "Friend", country = "Serbia", wins = 9))
    }

    fun startWithBot() {
        sessionRepository.setup = setup.copy(
            mode = GameMode.QUICK_MATCH,
            opponent = profileRepository.quickMatchOpponent(),
            bot = BotPersonality.randomForQuickMatch(),
        )
    }

    private companion object {
        const val SAMPLE_CODE = "KX-4821"
        const val MAX_CODE_LENGTH = 7
    }
}
