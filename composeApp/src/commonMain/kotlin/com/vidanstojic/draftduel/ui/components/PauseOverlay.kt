package com.vidanstojic.draftduel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.match_leave_confirm
import com.vidanstojic.draftduel.resources.match_paused_hint
import com.vidanstojic.draftduel.resources.match_paused_title
import com.vidanstojic.draftduel.resources.match_resume
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Pause menu over the whole match. The scrim is nearly opaque, so on a shared phone nobody can
 * study the table while the clock is stopped, and it swallows every tap meant for the table.
 */
@Composable
fun PauseOverlay(
    onResume: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DraftDuelTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(Res.string.match_paused_title), style = DraftDuelTheme.type.hero, color = colors.onScreen)
            Text(
                text = stringResource(Res.string.match_paused_hint),
                style = DraftDuelTheme.type.body,
                color = colors.muted,
                textAlign = TextAlign.Center,
            )
            DdPrimaryButton(stringResource(Res.string.match_resume), onClick = onResume, modifier = Modifier.fillMaxWidth())
            DdOutlinedButton(stringResource(Res.string.match_leave_confirm), onClick = onLeave, modifier = Modifier.fillMaxWidth())
        }
    }
}
