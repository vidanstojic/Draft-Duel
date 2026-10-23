package com.vidanstojic.draftduel.ui.screens.invite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.domain.model.Sport
import com.vidanstojic.draftduel.platform.rememberShareText
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.back
import com.vidanstojic.draftduel.resources.invite_bot_in
import com.vidanstojic.draftduel.resources.invite_code_label
import com.vidanstojic.draftduel.resources.invite_copied
import com.vidanstojic.draftduel.resources.invite_copy
import com.vidanstojic.draftduel.resources.invite_expires
import com.vidanstojic.draftduel.resources.invite_have_code
import com.vidanstojic.draftduel.resources.invite_join
import com.vidanstojic.draftduel.resources.invite_play_bot
import com.vidanstojic.draftduel.resources.invite_share
import com.vidanstojic.draftduel.resources.invite_share_message
import com.vidanstojic.draftduel.resources.invite_title
import com.vidanstojic.draftduel.resources.invite_waiting
import com.vidanstojic.draftduel.ui.components.DdIconButton
import com.vidanstojic.draftduel.ui.components.DdOutlinedButton
import com.vidanstojic.draftduel.ui.components.DdPrimaryButton
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.components.DdTopBar
import com.vidanstojic.draftduel.ui.components.InfoPill
import com.vidanstojic.draftduel.ui.components.SectionLabel
import com.vidanstojic.draftduel.ui.components.TopBarTitle
import com.vidanstojic.draftduel.ui.components.label
import com.vidanstojic.draftduel.ui.surface.PlaySurface
import com.vidanstojic.draftduel.ui.surface.surfaceStyle
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun InviteRoute(
    onBack: () -> Unit,
    onMatchReady: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { InviteViewModel(container.profileRepository, container.sessionRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    @Suppress("DEPRECATION") // LocalClipboard (suspend API) can replace this once it is stable.
    val clipboard = LocalClipboardManager.current
    val share = rememberShareText()
    val shareMessage = stringResource(Res.string.invite_share_message, state.link)

    InviteScreen(
        state = state,
        onBack = onBack,
        onCopy = {
            clipboard.setText(AnnotatedString(state.link))
            viewModel.onCopied()
        },
        onShare = { share(shareMessage) },
        onJoinCodeChange = viewModel::onJoinCodeChange,
        onJoin = {
            viewModel.startWithFriend()
            onMatchReady()
        },
        onPlayBot = {
            viewModel.startWithBot()
            onMatchReady()
        },
    )
}

@Composable
fun InviteScreen(
    state: InviteUiState,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onJoinCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
    onPlayBot: () -> Unit,
) {
    val surface = state.sport.surfaceStyle()
    DdScreen(surface = surface) {
        val colors = DraftDuelTheme.colors
        DdTopBar(
            leading = { DdIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back), onBack) },
            title = { TopBarTitle(stringResource(Res.string.invite_title), stringResource(state.sport.label())) },
        )
        PlaySurface(style = surface, modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(colors.panel, CircleShape)
                            .border(2.dp, colors.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = colors.accent, strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
                    }
                    InfoPill {
                        Text(stringResource(Res.string.invite_waiting), style = DraftDuelTheme.type.bodyStrong, color = colors.onScreen)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.panel, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionLabel(stringResource(Res.string.invite_code_label))
                    Text(state.code, style = DraftDuelTheme.type.monoStrong.copy(fontSize = 34.sp, letterSpacing = 3.sp), color = colors.accent)
                    Text(state.link, style = DraftDuelTheme.type.mono, color = colors.onScreen)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CopyButton(copied = state.copied, onClick = onCopy, modifier = Modifier.weight(1f))
                        ShareButton(onClick = onShare, modifier = Modifier.weight(1f))
                    }
                }

                InfoPill {
                    Text(
                        text = stringResource(Res.string.invite_expires, formatClock(state.expiresInSeconds)),
                        style = DraftDuelTheme.type.mono,
                        color = colors.muted,
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.joinCode,
                onValueChange = onJoinCodeChange,
                singleLine = true,
                placeholder = { Text(stringResource(Res.string.invite_have_code)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.outline,
                    focusedTextColor = colors.onScreen,
                    unfocusedTextColor = colors.onScreen,
                    cursorColor = colors.accent,
                    focusedPlaceholderColor = colors.muted,
                    unfocusedPlaceholderColor = colors.muted,
                ),
                modifier = Modifier.weight(1f),
            )
            DdPrimaryButton(
                text = stringResource(Res.string.invite_join),
                onClick = onJoin,
                enabled = state.canJoin,
                modifier = Modifier.width(96.dp),
            )
        }
        if (state.botOffered) {
            DdOutlinedButton(text = stringResource(Res.string.invite_play_bot), onClick = onPlayBot, modifier = Modifier.fillMaxWidth())
        } else {
            Text(
                text = stringResource(Res.string.invite_bot_in, formatClock(state.botOfferInSeconds)),
                style = DraftDuelTheme.type.mono,
                color = colors.muted,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun CopyButton(copied: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DraftDuelTheme.colors
    TextButton(
        onClick = onClick,
        modifier = modifier
            .height(44.dp)
            .border(1.5.dp, colors.outline, RoundedCornerShape(12.dp)),
    ) {
        if (copied) Icon(Icons.Filled.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(if (copied) Res.string.invite_copied else Res.string.invite_copy),
            style = DraftDuelTheme.type.bodyStrong,
            color = colors.onScreen,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun ShareButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DraftDuelTheme.colors
    TextButton(
        onClick = onClick,
        modifier = modifier
            .height(44.dp)
            .background(colors.accent, RoundedCornerShape(12.dp)),
    ) {
        Icon(Icons.Filled.Share, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(Res.string.invite_share),
            style = DraftDuelTheme.type.bodyStrong,
            color = colors.onAccent,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

private fun formatClock(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Preview
@Composable
private fun InviteScreenPreview() {
    InviteScreen(
        state = InviteUiState(
            sport = Sport.BASKETBALL,
            code = "KX-4821",
            link = "draftduel.gg/j/KX-4821",
            expiresInSeconds = 592,
            waitedSeconds = 8,
        ),
        onBack = {},
        onCopy = {},
        onShare = {},
        onJoinCodeChange = {},
        onJoin = {},
        onPlayBot = {},
    )
}
