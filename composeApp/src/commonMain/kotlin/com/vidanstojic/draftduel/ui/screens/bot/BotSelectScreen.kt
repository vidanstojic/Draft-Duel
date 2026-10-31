package com.vidanstojic.draftduel.ui.screens.bot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.domain.bot.BotDifficulty
import com.vidanstojic.draftduel.domain.bot.BotPersonality
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.back
import com.vidanstojic.draftduel.resources.bot_difficulty_easy
import com.vidanstojic.draftduel.resources.bot_difficulty_hard
import com.vidanstojic.draftduel.resources.bot_difficulty_medium
import com.vidanstojic.draftduel.resources.bot_offline_note
import com.vidanstojic.draftduel.resources.bot_opponents
import com.vidanstojic.draftduel.resources.bot_select_title
import com.vidanstojic.draftduel.resources.bot_start
import com.vidanstojic.draftduel.resources.bot_tagline_bargain_hunter
import com.vidanstojic.draftduel.resources.bot_tagline_casual_fan
import com.vidanstojic.draftduel.resources.bot_tagline_hustler
import com.vidanstojic.draftduel.resources.bot_tagline_scout
import com.vidanstojic.draftduel.resources.bot_tagline_star_chaser
import com.vidanstojic.draftduel.resources.bot_tagline_steady
import com.vidanstojic.draftduel.resources.mode_bot_title
import com.vidanstojic.draftduel.ui.components.DdIconButton
import com.vidanstojic.draftduel.ui.components.DdPrimaryButton
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.components.DdTopBar
import com.vidanstojic.draftduel.ui.components.ProfileBadge
import com.vidanstojic.draftduel.ui.components.SectionLabel
import com.vidanstojic.draftduel.ui.components.TopBarTitle
import com.vidanstojic.draftduel.ui.surface.SurfaceStyle
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BotSelectRoute(
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { BotSelectViewModel(container.profileRepository, container.sessionRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    BotSelectScreen(
        state = state,
        onBack = onBack,
        onSelect = viewModel::select,
        onStart = {
            viewModel.confirm()
            onStart()
        },
    )
}

@Composable
fun BotSelectScreen(
    state: BotSelectUiState,
    onBack: () -> Unit,
    onSelect: (BotPersonality) -> Unit,
    onStart: () -> Unit,
) {
    DdScreen(surface = SurfaceStyle.TABLE) {
        DdTopBar(
            leading = { DdIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back), onBack) },
            title = { TopBarTitle(stringResource(Res.string.mode_bot_title), stringResource(Res.string.bot_select_title)) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionLabel(stringResource(Res.string.bot_opponents), Modifier.padding(horizontal = 4.dp))
            state.bots.forEach { bot ->
                BotRow(bot = bot, difficulty = bot.difficultyFor(state.scoring, state.ruleSet), selected = bot == state.selected, onClick = { onSelect(bot) })
            }
            Text(
                text = stringResource(Res.string.bot_offline_note),
                style = DraftDuelTheme.type.mono,
                color = DraftDuelTheme.colors.muted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        DdPrimaryButton(
            text = stringResource(Res.string.bot_start, state.selected.nickname),
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BotRow(
    bot: BotPersonality,
    difficulty: BotDifficulty,
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
            .border(if (selected) 2.dp else 1.5.dp, if (selected) colors.accent else colors.outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileBadge(name = bot.nickname, color = badgeColor(difficulty), size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(bot.nickname, style = DraftDuelTheme.type.bodyStrong, color = colors.onScreen)
            Text(stringResource(bot.tagline()), style = DraftDuelTheme.type.caption, color = colors.muted)
        }
        DifficultyBadge(difficulty)
    }
}

/** Pips plus a word, so difficulty is never shown by color alone (spec §14). */
@Composable
private fun DifficultyBadge(difficulty: BotDifficulty) {
    val colors = DraftDuelTheme.colors
    val level = difficulty.ordinal + 1
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(BotDifficulty.entries.size) { index ->
                val filled = index < level
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (filled) colors.accent else Color.Transparent, CircleShape)
                        .border(1.dp, if (filled) colors.accent else colors.muted, CircleShape),
                )
            }
        }
        Text(stringResource(difficulty.label()), style = DraftDuelTheme.type.mono, color = colors.muted)
    }
}

@Composable
private fun badgeColor(difficulty: BotDifficulty): Color = when (difficulty) {
    BotDifficulty.EASY -> DraftDuelTheme.colors.secondaryBid.container
    BotDifficulty.MEDIUM -> DraftDuelTheme.colors.primaryBid.container
    BotDifficulty.HARD -> DraftDuelTheme.colors.opponent
}

private fun BotPersonality.tagline(): StringResource = when (this) {
    BotPersonality.STAR_CHASER -> Res.string.bot_tagline_star_chaser
    BotPersonality.CASUAL_FAN -> Res.string.bot_tagline_casual_fan
    BotPersonality.BARGAIN_HUNTER -> Res.string.bot_tagline_bargain_hunter
    BotPersonality.STEADY -> Res.string.bot_tagline_steady
    BotPersonality.HUSTLER -> Res.string.bot_tagline_hustler
    BotPersonality.SCOUT -> Res.string.bot_tagline_scout
}

private fun BotDifficulty.label(): StringResource = when (this) {
    BotDifficulty.EASY -> Res.string.bot_difficulty_easy
    BotDifficulty.MEDIUM -> Res.string.bot_difficulty_medium
    BotDifficulty.HARD -> Res.string.bot_difficulty_hard
}

@Preview
@Composable
private fun BotSelectScreenPreview() {
    DraftDuelTheme {
        BotSelectScreen(
            state = BotSelectUiState(selected = BotPersonality.STEADY),
            onBack = {},
            onSelect = {},
            onStart = {},
        )
    }
}
