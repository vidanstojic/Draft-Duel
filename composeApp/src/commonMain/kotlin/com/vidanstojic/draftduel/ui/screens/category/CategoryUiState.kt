package com.vidanstojic.draftduel.ui.screens.category

import androidx.compose.runtime.Immutable
import com.vidanstojic.draftduel.domain.match.RuleSet
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.Scoring
import com.vidanstojic.draftduel.domain.model.Theme

enum class PoolSourceType { RANDOM, CATEGORY, CUSTOM }

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
    val ruleSet: RuleSet = RuleSet.CLASSIC,
) {
    /** Five slots are a rating-pack variant; recipes have their own skip rule. */
    val ruleSetsAvailable: Boolean get() = selectedTheme.scoring == Scoring.RATINGS

    /** Hand-picked pools exist only in friend matches (spec §5). */
    val customAllowed: Boolean get() = mode == GameMode.FRIEND

    val canContinue: Boolean
        get() = when (sourceType) {
            PoolSourceType.RANDOM -> true
            PoolSourceType.CATEGORY -> selectedCategoryId != null
            PoolSourceType.CUSTOM -> customAllowed
        }
}
