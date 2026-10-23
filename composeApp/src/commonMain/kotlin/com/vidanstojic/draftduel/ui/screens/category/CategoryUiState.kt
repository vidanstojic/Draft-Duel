package com.vidanstojic.draftduel.ui.screens.category

import androidx.compose.runtime.Immutable
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.Sport

enum class PoolSourceType { RANDOM, CATEGORY, CUSTOM }

@Immutable
data class SportOptionUi(
    val sport: Sport,
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
    val sports: List<SportOptionUi>,
    val selectedSport: Sport,
    val sourceType: PoolSourceType = PoolSourceType.RANDOM,
    val categories: List<CategoryOptionUi> = emptyList(),
    val selectedCategoryId: String? = null,
) {
    /** Hand-picked pools exist only in friend matches (spec §5). */
    val customAllowed: Boolean get() = mode == GameMode.FRIEND

    val canContinue: Boolean
        get() = when (sourceType) {
            PoolSourceType.RANDOM -> true
            PoolSourceType.CATEGORY -> selectedCategoryId != null
            PoolSourceType.CUSTOM -> customAllowed
        }
}
