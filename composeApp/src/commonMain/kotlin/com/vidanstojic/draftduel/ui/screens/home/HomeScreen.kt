package com.vidanstojic.draftduel.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vidanstojic.draftduel.domain.model.GameMode
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.app_name
import com.vidanstojic.draftduel.resources.got_it
import com.vidanstojic.draftduel.resources.home_tagline
import com.vidanstojic.draftduel.resources.home_tagline_accent
import com.vidanstojic.draftduel.resources.how_to_play
import com.vidanstojic.draftduel.resources.how_to_play_rules
import com.vidanstojic.draftduel.resources.mode_find_subtitle
import com.vidanstojic.draftduel.resources.mode_find_title
import com.vidanstojic.draftduel.resources.mode_friend_subtitle
import com.vidanstojic.draftduel.resources.mode_friend_title
import com.vidanstojic.draftduel.resources.mode_local_subtitle
import com.vidanstojic.draftduel.resources.mode_local_title
import com.vidanstojic.draftduel.resources.settings
import com.vidanstojic.draftduel.ui.components.ChipDisc
import com.vidanstojic.draftduel.ui.components.DdIconButton
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.components.DdTopBar
import com.vidanstojic.draftduel.ui.components.HiddenRating
import com.vidanstojic.draftduel.ui.components.MiniCard
import com.vidanstojic.draftduel.ui.components.PileCard
import com.vidanstojic.draftduel.ui.surface.PlaySurface
import com.vidanstojic.draftduel.ui.surface.SurfaceStyle
import com.vidanstojic.draftduel.ui.theme.CardColors
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HomeRoute(onModeSelected: (GameMode) -> Unit) {
    HomeScreen(onModeSelected = onModeSelected, onSettings = {})
}

@Composable
fun HomeScreen(
    onModeSelected: (GameMode) -> Unit,
    onSettings: () -> Unit,
) {
    var showRules by rememberSaveable { mutableStateOf(false) }
    val colors = DraftDuelTheme.colors

    DdScreen(surface = SurfaceStyle.TABLE) {
        DdTopBar(
            leading = { Logo() },
            trailing = { DdIconButton(Icons.Filled.Settings, stringResource(Res.string.settings), onSettings) },
            title = {},
        )
        HeroTable()
        Spacer(Modifier.height(4.dp))
        ModeCard(
            title = stringResource(Res.string.mode_find_title),
            subtitle = stringResource(Res.string.mode_find_subtitle),
            icon = Icons.Filled.Search,
            iconBackground = colors.onAccent,
            highlighted = true,
            onClick = { onModeSelected(GameMode.QUICK_MATCH) },
        )
        ModeCard(
            title = stringResource(Res.string.mode_local_title),
            subtitle = stringResource(Res.string.mode_local_subtitle),
            icon = Icons.Filled.Person,
            iconBackground = colors.secondaryBid.container,
            onClick = { onModeSelected(GameMode.SAME_DEVICE) },
        )
        ModeCard(
            title = stringResource(Res.string.mode_friend_title),
            subtitle = stringResource(Res.string.mode_friend_subtitle),
            icon = Icons.Filled.Share,
            iconBackground = colors.primaryBid.container,
            onClick = { onModeSelected(GameMode.FRIEND) },
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { showRules = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(Res.string.how_to_play), style = DraftDuelTheme.type.mono, color = colors.muted)
        }
    }

    if (showRules) {
        AlertDialog(
            onDismissRequest = { showRules = false },
            confirmButton = {
                TextButton(onClick = { showRules = false }) { Text(stringResource(Res.string.got_it)) }
            },
            title = { Text(stringResource(Res.string.how_to_play)) },
            text = { Text(stringResource(Res.string.how_to_play_rules)) },
        )
    }
}

@Composable
private fun Logo() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ChipDisc(color = DraftDuelTheme.colors.primaryBid.container, ringColor = CardColors.Paper, size = 34.dp)
        Text(stringResource(Res.string.app_name), style = DraftDuelTheme.type.headline, color = DraftDuelTheme.colors.onScreen)
    }
}

@Composable
private fun HeroTable() {
    PlaySurface(
        style = SurfaceStyle.TABLE,
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                MiniCard(PileCard.Back(paidPrice = 6), width = 60.dp, modifier = Modifier.offset(x = (-44).dp).rotate(-12f))
                MiniCard(PileCard.Back(paidPrice = 3), width = 60.dp, modifier = Modifier.offset(x = 44.dp).rotate(12f))
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 98.dp)
                        .background(CardColors.Paper, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    HiddenRating(size = 42.dp)
                }
            }
            Text(
                text = stringResource(Res.string.home_tagline),
                style = DraftDuelTheme.type.bodyStrong,
                color = DraftDuelTheme.colors.onScreen,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.home_tagline_accent),
                style = DraftDuelTheme.type.bodyStrong,
                color = DraftDuelTheme.colors.accent,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBackground: Color,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    val colors = DraftDuelTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val container = if (highlighted) colors.accent else colors.panel
    val content = if (highlighted) colors.onAccent else colors.onScreen
    val subtitleColor = if (highlighted) colors.onAccent else colors.muted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .then(if (highlighted) Modifier else Modifier.border(1.5.dp, colors.outline, shape))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (highlighted) colors.accent else Color.White)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = DraftDuelTheme.type.title, color = content)
            Text(subtitle, style = DraftDuelTheme.type.caption, color = subtitleColor)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = content)
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    DraftDuelTheme {
        HomeScreen(onModeSelected = {}, onSettings = {})
    }
}
