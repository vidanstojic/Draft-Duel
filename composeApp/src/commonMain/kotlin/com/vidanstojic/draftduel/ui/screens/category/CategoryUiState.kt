package com.vidanstojic.draftduel.ui.screens.category

import androidx.compose.runtime.Immutable
import com.vidanstojic.draftduel.domain.match.MatchRules
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.Scoring
import com.vidanstojic.draftduel.domain.model.Theme

enum class PoolSourceType { RANDOM, CATEGORY, CUSTOM }

/** Default and Classic are fixed rule sets; Custom lets the player set every parameter. */
enum class RulesPreset { DEFAULT, CLASSIC, CUSTOM }

@Immutable
data class ThemeOptionUi(
    val theme: Theme,
    val available: Boolean,
)

@Immutable
data class CategoryOptionUi(
    val id: String,
    val name: String,
    val athleteCount: Int,
)

@Immutable
data class CategoryUiState(
    val mode: GameMode,
    val themes: List<ThemeOptionUi>,
    val selectedTheme: Theme,
    val sourceType: PoolSourceType = PoolSourceType.RANDOM,
    val categories: List<CategoryOptionUi> = emptyList(),
    val selectedCategoryId: String? = null,
    val rulesPreset: RulesPreset = RulesPreset.DEFAULT,
    /** Last values of the Custom editor, kept while switching presets. */
    val customRules: MatchRules = MatchRules.DEFAULT,
) {
    /** "Find opponent" will match strangers, who must play the same rules; Custom is for the other modes. */
    val customRulesAllowed: Boolean get() = mode != GameMode.QUICK_MATCH

    /** Food keeps its own rule (the opener may skip), so card limits and hand-overs do not apply. */
    val isRecipe: Boolean get() = selectedTheme.scoring == Scoring.RECIPE

    val rules: MatchRules
        get() = when (rulesPreset) {
            RulesPreset.DEFAULT -> MatchRules.DEFAULT
            RulesPreset.CLASSIC -> MatchRules.CLASSIC
            RulesPreset.CUSTOM -> customRules
        }

    /** Hand-picked pools exist only in friend matches (spec §5). */
    val customAllowed: Boolean get() = mode == GameMode.FRIEND

    val canContinue: Boolean
        get() = when (sourceType) {
            PoolSourceType.RANDOM -> true
            PoolSourceType.CATEGORY -> selectedCategoryId != null
            PoolSourceType.CUSTOM -> customAllowed
        }
}
