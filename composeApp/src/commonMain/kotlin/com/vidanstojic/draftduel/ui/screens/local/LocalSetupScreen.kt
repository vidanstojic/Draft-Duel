package com.vidanstojic.draftduel.ui.screens.local

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.domain.model.LocalLayout
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.back
import com.vidanstojic.draftduel.resources.local_layout_label
import com.vidanstojic.draftduel.resources.local_pass_subtitle
import com.vidanstojic.draftduel.resources.local_pass_title
import com.vidanstojic.draftduel.resources.local_player_one
import com.vidanstojic.draftduel.resources.local_player_one_default
import com.vidanstojic.draftduel.resources.local_player_two
import com.vidanstojic.draftduel.resources.local_player_two_default
import com.vidanstojic.draftduel.resources.local_players_label
import com.vidanstojic.draftduel.resources.local_ratings_note
import com.vidanstojic.draftduel.resources.local_split_subtitle
import com.vidanstojic.draftduel.resources.local_split_title
import com.vidanstojic.draftduel.resources.local_start
import com.vidanstojic.draftduel.resources.mode_local_title
import com.vidanstojic.draftduel.ui.components.DdIconButton
import com.vidanstojic.draftduel.ui.components.DdPrimaryButton
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.components.DdTopBar
import com.vidanstojic.draftduel.ui.components.SectionLabel
import com.vidanstojic.draftduel.ui.components.TopBarTitle
import com.vidanstojic.draftduel.ui.components.label
import com.vidanstojic.draftduel.ui.surface.SurfaceStyle
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun LocalSetupRoute(
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val container = LocalAppContainer.current
    val playerOne = stringResource(Res.string.local_player_one_default)
    val playerTwo = stringResource(Res.string.local_player_two_default)
    val viewModel = viewModel { LocalSetupViewModel(container.sessionRepository, playerOne, playerTwo) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LocalSetupScreen(
        state = state,
        onBack = onBack,
        onPlayerOneChange = viewModel::onPlayerOneChange,
        onPlayerTwoChange = viewModel::onPlayerTwoChange,
        onLayoutSelected = viewModel::onLayoutSelected,
        onStart = {
            viewModel.confirm()
            onStart()
        },
    )
}

@Composable
fun LocalSetupScreen(
    state: LocalSetupUiState,
    onBack: () -> Unit,
    onPlayerOneChange: (String) -> Unit,
    onPlayerTwoChange: (String) -> Unit,
    onLayoutSelected: (LocalLayout) -> Unit,
    onStart: () -> Unit,
) {
    DdScreen(surface = SurfaceStyle.TABLE) {
        val colors = DraftDuelTheme.colors
        DdTopBar(
            leading = { DdIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back), onBack) },
            title = { TopBarTitle(stringResource(Res.string.mode_local_title), stringResource(state.theme.label())) },
        )
        SectionLabel(stringResource(Res.string.local_players_label), Modifier.padding(start = 4.dp, top = 8.dp))
        NameField(
            value = state.playerOne,
            onValueChange = onPlayerOneChange,
            label = stringResource(Res.string.local_player_one),
            dot = colors.accent,
        )
        NameField(
            value = state.playerTwo,
            onValueChange = onPlayerTwoChange,
            label = stringResource(Res.string.local_player_two),
            dot = colors.opponent,
        )
        SectionLabel(stringResource(Res.string.local_layout_label), Modifier.padding(start = 4.dp, top = 8.dp))
        LayoutOption(
            title = stringResource(Res.string.local_split_title),
            subtitle = stringResource(Res.string.local_split_subtitle),
            selected = state.layout == LocalLayout.SPLIT_SCREEN,
            pictogram = { SplitPictogram() },
            onClick = { onLayoutSelected(LocalLayout.SPLIT_SCREEN) },
        )
        LayoutOption(
            title = stringResource(Res.string.local_pass_title),
            subtitle = stringResource(Res.string.local_pass_subtitle),
            selected = state.layout == LocalLayout.PASS_DEVICE,
            pictogram = { PassPictogram() },
            onClick = { onLayoutSelected(LocalLayout.PASS_DEVICE) },
        )
        Text(
            text = stringResource(Res.string.local_ratings_note),
            style = DraftDuelTheme.type.caption,
            color = colors.muted,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.weight(1f))
        DdPrimaryButton(
            text = stringResource(Res.string.local_start),
            onClick = onStart,
            enabled = state.canStart,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    dot: Color,
) {
    val colors = DraftDuelTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(label) },
        leadingIcon = { Box(Modifier.size(12.dp).background(dot, CircleShape)) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.outline,
            focusedTextColor = colors.onScreen,
            unfocusedTextColor = colors.onScreen,
            focusedLabelColor = colors.accent,
            unfocusedLabelColor = colors.muted,
            cursorColor = colors.accent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LayoutOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    pictogram: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val colors = DraftDuelTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.panel)
            .border(if (selected) 2.dp else 1.5.dp, if (selected) colors.accent else colors.outline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        pictogram()
        Column(Modifier.weight(1f)) {
            Text(title, style = DraftDuelTheme.type.title, color = colors.onScreen)
            Text(subtitle, style = DraftDuelTheme.type.caption, color = colors.muted)
        }
    }
}

/** A phone lying flat: two halves facing opposite ways. */
@Composable
private fun SplitPictogram() {
    val colors = DraftDuelTheme.colors
    Canvas(Modifier.size(width = 40.dp, height = 56.dp)) {
        val stroke = 2.dp.toPx()
        drawRoundRect(colors.onScreen, cornerRadius = CornerRadius(6.dp.toPx()), style = Stroke(stroke))
        drawLine(colors.onScreen, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), stroke)
        drawRect(colors.opponent, Offset(6.dp.toPx(), 6.dp.toPx()), Size(size.width - 12.dp.toPx(), size.height / 2 - 10.dp.toPx()))
        drawRect(colors.accent, Offset(6.dp.toPx(), size.height / 2 + 4.dp.toPx()), Size(size.width - 12.dp.toPx(), size.height / 2 - 10.dp.toPx()))
    }
}

/** Two phones with an arrow between them. */
@Composable
private fun PassPictogram() {
    val colors = DraftDuelTheme.colors
    Canvas(Modifier.size(width = 40.dp, height = 56.dp)) {
        val stroke = 2.dp.toPx()
        val phone = Size(size.width * 0.42f, size.height * 0.6f)
        drawRoundRect(colors.onScreen, Offset(0f, size.height * 0.2f), phone, CornerRadius(4.dp.toPx()), style = Stroke(stroke))
        drawRoundRect(colors.accent, Offset(size.width - phone.width, size.height * 0.2f), phone, CornerRadius(4.dp.toPx()))
        val y = size.height * 0.12f
        drawLine(colors.onScreen, Offset(size.width * 0.2f, y), Offset(size.width * 0.8f, y), stroke)
        drawLine(colors.onScreen, Offset(size.width * 0.8f, y), Offset(size.width * 0.68f, y - 5.dp.toPx()), stroke)
        drawLine(colors.onScreen, Offset(size.width * 0.8f, y), Offset(size.width * 0.68f, y + 5.dp.toPx()), stroke)
    }
}

@Preview
@Composable
private fun LocalSetupScreenPreview() {
    LocalSetupScreen(
        state = LocalSetupUiState(theme = Theme.BASKETBALL, playerOne = "Luka", playerTwo = "Ana"),
        onBack = {},
        onPlayerOneChange = {},
        onPlayerTwoChange = {},
        onLayoutSelected = {},
        onStart = {},
    )
}
