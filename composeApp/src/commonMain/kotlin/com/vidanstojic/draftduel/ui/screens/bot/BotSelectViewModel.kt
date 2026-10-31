package com.vidanstojic.draftduel.ui.screens.bot

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.vidanstojic.draftduel.domain.bot.BotPersonality
import com.vidanstojic.draftduel.domain.match.RuleSet
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.MatchSetup
import com.vidanstojic.draftduel.domain.model.PlayerProfile
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Scoring
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.domain.repository.MatchSessionRepository
import com.vidanstojic.draftduel.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class BotSelectUiState(
    /** Difficulty labels depend on how the chosen theme scores. */
    val scoring: Scoring = Scoring.RATINGS,
    val ruleSet: RuleSet = RuleSet.CLASSIC,
    val bots: List<BotPersonality> = BotPersonality.entries,
    val selected: BotPersonality,
)

/** "Play vs bot": the player picks who to face. Runs fully on the phone, so it also works offline. */
class BotSelectViewModel(
    profileRepository: ProfileRepository,
    private val sessionRepository: MatchSessionRepository,
) : ViewModel() {

    private val setup: MatchSetup = sessionRepository.setup ?: MatchSetup(
        mode = GameMode.VS_BOT,
        theme = Theme.FOOTBALL,
        poolSource = PoolSource.Random,
        you = profileRepository.currentPlayer(),
    )

    private val _state = MutableStateFlow(
        BotSelectUiState(scoring = setup.theme.scoring, ruleSet = setup.ruleSet, selected = setup.bot ?: DEFAULT_BOT),
    )
    val state: StateFlow<BotSelectUiState> = _state.asStateFlow()

    fun select(personality: BotPersonality) {
        _state.update { it.copy(selected = personality) }
    }

    /** Stores the chosen bot as the opponent; the match screen builds it from the setup. */
    fun confirm() {
        val bot = _state.value.selected
        sessionRepository.setup = setup.copy(
            opponent = PlayerProfile(nickname = bot.nickname, country = "", wins = 0),
            bot = bot,
        )
    }

    private companion object {
        val DEFAULT_BOT = BotPersonality.STEADY
    }
}
