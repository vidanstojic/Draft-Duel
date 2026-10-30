package com.vidanstojic.draftduel.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.domain.model.Scoring
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.back
import com.vidanstojic.draftduel.resources.category_athletes
import com.vidanstojic.draftduel.resources.category_coming_soon
import com.vidanstojic.draftduel.resources.category_custom_friend_only
import com.vidanstojic.draftduel.resources.category_custom_hint
import com.vidanstojic.draftduel.resources.category_new_match
import com.vidanstojic.draftduel.resources.category_pick_categories
import com.vidanstojic.draftduel.resources.category_pool
import com.vidanstojic.draftduel.resources.category_recipe_hint
import com.vidanstojic.draftduel.resources.category_theme
import com.vidanstojic.draftduel.resources.category_title
import com.vidanstojic.draftduel.resources.cta_choose_bot
import com.vidanstojic.draftduel.resources.cta_continue
import com.vidanstojic.draftduel.resources.cta_create_invite
import com.vidanstojic.draftduel.resources.mode_find_title
import com.vidanstojic.draftduel.resources.source_category
import com.vidanstojic.draftduel.resources.source_custom
import com.vidanstojic.draftduel.resources.source_random
import com.vidanstojic.draftduel.ui.components.DdIconButton
import com.vidanstojic.draftduel.ui.components.DdPrimaryButton
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.components.DdTopBar
import com.vidanstojic.draftduel.ui.components.SectionLabel
import com.vidanstojic.draftduel.ui.components.TopBarTitle
import com.vidanstojic.draftduel.ui.components.label
import com.vidanstojic.draftduel.ui.surface.SurfacePreview
import com.vidanstojic.draftduel.ui.surface.SurfaceStyle
import com.vidanstojic.draftduel.ui.surface.surfaceStyle
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CategoryRoute(
    mode: GameMode,
    onBack: () -> Unit,
    onContinue: (GameMode) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel {
        CategoryViewModel(mode, container.packRepository, container.profileRepository, container.sessionRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategoryScreen(
        state = state,
        onBack = onBack,
        onThemeSelected = viewModel::selectTheme,
        onSourceSelected = viewModel::selectSource,
        onCategorySelected = viewModel::selectCategory,
        onContinue = { onContinue(viewModel.confirm()) },
    )
}

@Composable
fun CategoryScreen(
    state: CategoryUiState,
    onBack: () -> Unit,
    onThemeSelected: (Theme) -> Unit,
    onSourceSelected: (PoolSourceType) -> Unit,
    onCategorySelected: (String) -> Unit,
    onContinue: () -> Unit,
) {
    DdScreen(surface = SurfaceStyle.TABLE) {
        DdTopBar(
            leading = { DdIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back), onBack) },
            title = { TopBarTitle(stringResource(Res.string.category_new_match), stringResource(Res.string.category_title)) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel(stringResource(Res.string.category_theme), Modifier.padding(horizontal = 4.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.themes.forEach { option ->
                    ThemeTile(
                        option = option,
                        selected = option.theme == state.selectedTheme,
                        onClick = { onThemeSelected(option.theme) },
                        modifier = Modifier.width(112.dp),
                    )
                }
            }
            if (state.selectedTheme.scoring == Scoring.RECIPE) {
                Text(
                    text = stringResource(Res.string.category_recipe_hint),
                    style = DraftDuelTheme.type.caption,
                    color = DraftDuelTheme.colors.muted,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            SectionLabel(stringResource(Res.string.category_pool), Modifier.padding(start = 4.dp, top = 6.dp))
            SourceSelector(
                selected = state.sourceType,
                customAllowed = state.customAllowed,
                onSelected = onSourceSelected,
            )

            when (state.sourceType) {
                PoolSourceType.CATEGORY, PoolSourceType.RANDOM -> {
                    SectionLabel(stringResource(Res.string.category_pick_categories), Modifier.padding(start = 4.dp, top = 6.dp))
                    state.categories.forEach { category ->
                        CategoryRow(
                            category = category,
                            selected = state.sourceType == PoolSourceType.CATEGORY && category.id == state.selectedCategoryId,
                            onClick = { onCategorySelected(category.id) },
                        )
                    }
                }
                PoolSourceType.CUSTOM -> Text(
                    text = stringResource(Res.string.category_custom_hint),
                    style = DraftDuelTheme.type.caption,
                    color = DraftDuelTheme.colors.muted,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            if (!state.customAllowed) {
                Text(
                    text = stringResource(Res.string.category_custom_friend_only),
                    style = DraftDuelTheme.type.mono,
                    color = DraftDuelTheme.colors.muted,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        DdPrimaryButton(
            text = stringResource(ctaFor(state.mode)),
            onClick = onContinue,
            enabled = state.canContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun ctaFor(mode: GameMode): StringResource = when (mode) {
    GameMode.QUICK_MATCH -> Res.string.mode_find_title
    GameMode.FRIEND -> Res.string.cta_create_invite
    GameMode.SAME_DEVICE -> Res.string.cta_continue
    GameMode.VS_BOT -> Res.string.cta_choose_bot
}

@Composable
private fun ThemeTile(
    option: ThemeOptionUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DraftDuelTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .alpha(if (option.available) 1f else 0.55f)
            .clip(shape)
            .background(colors.panel)
            .border(if (selected) 2.dp else 1.5.dp, if (selected) colors.accent else colors.outline, shape)
            .selectable(selected = selected, enabled = option.available, role = Role.RadioButton, onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SurfacePreview(
            style = option.theme.surfaceStyle(),
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(stringResource(option.theme.label()), style = DraftDuelTheme.type.bodyStrong, color = colors.onScreen)
                if (!option.available) {
                    Text(stringResource(Res.string.category_coming_soon), style = DraftDuelTheme.type.mono, color = colors.muted)
                }
            }
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SourceSelector(
    selected: PoolSourceType,
    customAllowed: Boolean,
    onSelected: (PoolSourceType) -> Unit,
) {
    val colors = DraftDuelTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.panel)
            .border(1.5.dp, colors.outline, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PoolSourceType.entries.forEach { type ->
            val isSelected = type == selected
            val enabled = type != PoolSourceType.CUSTOM || customAllowed
            val label = when (type) {
                PoolSourceType.RANDOM -> Res.string.source_random
                PoolSourceType.CATEGORY -> Res.string.source_category
                PoolSourceType.CUSTOM -> Res.string.source_custom
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSelected) colors.accent else colors.panel.copy(alpha = 0f))
                    .selectable(selected = isSelected, enabled = enabled, role = Role.Tab, onClick = { onSelected(type) })
                    .alpha(if (enabled) 1f else 0.4f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(label),
                    style = DraftDuelTheme.type.bodyStrong,
                    color = if (isSelected) colors.onAccent else colors.onScreen,
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryOptionUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = DraftDuelTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.panel)
            .border(1.5.dp, if (selected) colors.accent else colors.outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(2.dp, if (selected) colors.accent else colors.muted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).background(colors.accent, CircleShape))
        }
        Column(Modifier.weight(1f)) {
            Text(category.name, style = DraftDuelTheme.type.bodyStrong, color = colors.onScreen)
            Text(
                text = stringResource(Res.string.category_athletes, category.athleteCount),
                style = DraftDuelTheme.type.mono,
                color = colors.muted,
            )
        }
    }
}

@Preview
@Composable
private fun CategoryScreenPreview() {
    DraftDuelTheme {
        CategoryScreen(
            state = CategoryUiState(
                mode = GameMode.QUICK_MATCH,
                themes = listOf(
                    ThemeOptionUi(Theme.FOOTBALL, true),
                    ThemeOptionUi(Theme.BASKETBALL, true),
                    ThemeOptionUi(Theme.TENNIS, false),
                ),
                selectedTheme = Theme.FOOTBALL,
                sourceType = PoolSourceType.CATEGORY,
                categories = listOf(
                    CategoryOptionUi("legends", "Legends", 10),
                    CategoryOptionUi("premier-league", "Premier League", 9),
                ),
                selectedCategoryId = "legends",
            ),
            onBack = {},
            onThemeSelected = {},
            onSourceSelected = {},
            onCategorySelected = {},
            onContinue = {},
        )
    }
}
