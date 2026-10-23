package com.vidanstojic.draftduel.ui.screens.matchmaking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.domain.model.PlayerProfile
import com.vidanstojic.draftduel.domain.model.Sport
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.back
import com.vidanstojic.draftduel.resources.cancel
import com.vidanstojic.draftduel.resources.find_found
import com.vidanstojic.draftduel.resources.find_rules_summary
import com.vidanstojic.draftduel.resources.find_searching
import com.vidanstojic.draftduel.resources.find_starting
import com.vidanstojic.draftduel.resources.find_title
import com.vidanstojic.draftduel.resources.profile_meta
import com.vidanstojic.draftduel.resources.versus
import com.vidanstojic.draftduel.resources.you
import com.vidanstojic.draftduel.ui.components.DdIconButton
import com.vidanstojic.draftduel.ui.components.DdOutlinedButton
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.components.DdTopBar
import com.vidanstojic.draftduel.ui.components.InfoPill
import com.vidanstojic.draftduel.ui.components.ProfileBadge
import com.vidanstojic.draftduel.ui.components.TopBarTitle
import com.vidanstojic.draftduel.ui.components.label
import com.vidanstojic.draftduel.ui.surface.PlaySurface
import com.vidanstojic.draftduel.ui.surface.surfaceStyle
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun FindOpponentRoute(
    onCancel: () -> Unit,
    onMatchReady: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { FindOpponentViewModel(container.profileRepository, container.sessionRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.ready) {
        if (state.ready) onMatchReady()
    }
    FindOpponentScreen(state = state, onCancel = onCancel)
}

@Composable
fun FindOpponentScreen(
    state: FindOpponentUiState,
    onCancel: () -> Unit,
) {
    val surface = state.sport.surfaceStyle()
    DdScreen(surface = surface) {
        val colors = DraftDuelTheme.colors
        DdTopBar(
            leading = { DdIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back), onCancel) },
            title = { TopBarTitle(stringResource(Res.string.find_title), stringResource(state.sport.label())) },
        )
        PlaySurface(style = surface, modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val opponent = state.opponent
                if (opponent == null) {
                    SearchingSeat()
                } else {
                    Seat(profile = opponent, color = colors.opponent, nameOnTop = false)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    VersusDisc()
                    InfoPill {
                        if (opponent != null) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = stringResource(if (opponent == null) Res.string.find_searching else Res.string.find_found),
                            style = DraftDuelTheme.type.bodyStrong,
                            color = colors.onScreen,
                        )
                        Text(formatElapsed(state.elapsedSeconds), style = DraftDuelTheme.type.mono, color = colors.muted)
                    }
                    val startsIn = state.startsIn
                    if (startsIn != null && startsIn > 0) {
                        InfoPill {
                            Text(stringResource(Res.string.find_starting, startsIn), style = DraftDuelTheme.type.mono, color = colors.onScreen)
                        }
                    }
                }
                Seat(
                    profile = state.you.copy(nickname = stringResource(Res.string.you)),
                    color = colors.primaryBid.container,
                    nameOnTop = true,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.find_rules_summary), style = DraftDuelTheme.type.mono, color = colors.muted)
            DdOutlinedButton(text = stringResource(Res.string.cancel), onClick = onCancel, modifier = Modifier.width(120.dp))
        }
    }
}

@Composable
private fun Seat(profile: PlayerProfile, color: Color, nameOnTop: Boolean) {
    val colors = DraftDuelTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val name: @Composable () -> Unit = {
            InfoPill { Text(profile.nickname, style = DraftDuelTheme.type.title, color = colors.onScreen) }
        }
        if (nameOnTop) name()
        ProfileBadge(name = profile.nickname, color = color, size = 76.dp)
        if (!nameOnTop) name()
        Text(
            text = stringResource(Res.string.profile_meta, profile.country, profile.wins),
            style = DraftDuelTheme.type.mono,
            color = colors.muted,
        )
    }
}

@Composable
private fun SearchingSeat() {
    val colors = DraftDuelTheme.colors
    Box(
        modifier = Modifier
            .size(76.dp)
            .background(colors.panel, CircleShape)
            .border(2.dp, colors.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = colors.accent, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun VersusDisc() {
    val colors = DraftDuelTheme.colors
    Box(
        modifier = Modifier
            .size(112.dp)
            .border(2.dp, colors.accent.copy(alpha = 0.25f), CircleShape)
            .padding(14.dp)
            .border(2.dp, colors.accent.copy(alpha = 0.45f), CircleShape)
            .padding(14.dp)
            .background(colors.accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(Res.string.versus), style = DraftDuelTheme.type.headline, color = colors.onAccent)
    }
}

private fun formatElapsed(seconds: Int): String = "0:${seconds.toString().padStart(2, '0')}"

@Preview
@Composable
private fun FindOpponentScreenPreview() {
    FindOpponentScreen(
        state = FindOpponentUiState(
            sport = Sport.FOOTBALL,
            you = PlayerProfile("You", "Serbia", 12),
            elapsedSeconds = 3,
            opponent = PlayerProfile("Marco_23", "Italy", 41),
            startsIn = 3,
        ),
        onCancel = {},
    )
}
