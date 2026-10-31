package com.vidanstojic.draftduel.ui.screens.category

import androidx.lifecycle.ViewModel
import com.vidanstojic.draftduel.domain.match.RuleSet
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.MatchSetup
import com.vidanstojic.draftduel.domain.model.PoolSource
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.domain.repository.MatchSessionRepository
import com.vidanstojic.draftduel.domain.repository.PackRepository
import com.vidanstojic.draftduel.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CategoryViewModel(
    mode: GameMode,
    private val packRepository: PackRepository,
    private val profileRepository: ProfileRepository,
    private val sessionRepository: MatchSessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState(mode))
    val state: StateFlow<CategoryUiState> = _state.asStateFlow()

    fun selectTheme(theme: Theme) {
        if (packRepository.pack(theme) == null) return
        _state.update {
            it.copy(
                selectedTheme = theme,
                categories = categoriesOf(theme),
                selectedCategoryId = null,
                sourceType = if (it.sourceType == PoolSourceType.CATEGORY) PoolSourceType.RANDOM else it.sourceType,
            )
        }
    }

    fun selectRuleSet(ruleSet: RuleSet) {
        _state.update { if (it.ruleSetsAvailable) it.copy(ruleSet = ruleSet) else it }
    }

    fun selectSource(type: PoolSourceType) {
        _state.update { current ->
            if (type == PoolSourceType.CUSTOM && !current.customAllowed) return@update current
            current.copy(
                sourceType = type,
                selectedCategoryId = current.selectedCategoryId ?: current.categories.firstOrNull()?.id,
            )
        }
    }

    fun selectCategory(categoryId: String) {
        _state.update { it.copy(sourceType = PoolSourceType.CATEGORY, selectedCategoryId = categoryId) }
    }

    /** Stores the setup for the next screen. Returns the mode so the caller can navigate. */
    fun confirm(): GameMode {
        val current = _state.value
        val source = when (current.sourceType) {
            PoolSourceType.RANDOM -> PoolSource.Random
            PoolSourceType.CATEGORY -> current.selectedCategoryId?.let { PoolSource.ByCategory(it) } ?: PoolSource.Random
            PoolSourceType.CUSTOM -> PoolSource.Custom
        }
        sessionRepository.setup = MatchSetup(
            mode = current.mode,
            theme = current.selectedTheme,
            poolSource = source,
            you = profileRepository.currentPlayer(),
            ruleSet = if (current.ruleSetsAvailable) current.ruleSet else RuleSet.CLASSIC,
        )
        return current.mode
    }

    private fun initialState(mode: GameMode): CategoryUiState {
        val themes = packRepository.themes().map { ThemeOptionUi(it, available = packRepository.pack(it) != null) }
        val first = themes.first { it.available }.theme
        return CategoryUiState(
            mode = mode,
            themes = themes,
            selectedTheme = first,
            categories = categoriesOf(first),
        )
    }

    private fun categoriesOf(theme: Theme): List<CategoryOptionUi> {
        val pack = packRepository.pack(theme) ?: return emptyList()
        return pack.categories.map { CategoryOptionUi(it.id, it.name, pack.cardsIn(it.id).size) }
    }
}
