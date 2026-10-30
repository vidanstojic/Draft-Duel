package com.vidanstojic.draftduel.ui.screens.local

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vidanstojic.draftduel.data.fake.FakePacks
import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.resources.handoff_hidden_note
import com.vidanstojic.draftduel.resources.handoff_leads_at
import com.vidanstojic.draftduel.resources.handoff_new_card
import com.vidanstojic.draftduel.resources.handoff_paused
import com.vidanstojic.draftduel.resources.handoff_ready
import com.vidanstojic.draftduel.resources.handoff_title
import com.vidanstojic.draftduel.ui.components.DdPrimaryButton
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.components.InfoPill
import com.vidanstojic.draftduel.ui.components.SectionLabel
import com.vidanstojic.draftduel.ui.surface.PlaySurface
import com.vidanstojic.draftduel.ui.surface.surfaceStyle
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Covers the table between turns so the next player does not see the previous player's cards. */
@Composable
fun HandoffScreen(
    handoff: HandoffUi,
    theme: Theme,
    onReady: () -> Unit,
) {
    val surface = theme.surfaceStyle()
    DdScreen(surface = surface) {
        val colors = DraftDuelTheme.colors
        SectionLabel(stringResource(Res.string.handoff_paused), Modifier.align(Alignment.CenterHorizontally))
        PlaySurface(style = surface, modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            ) {
                HandoffPictogram()
                InfoPill {
                    Text(stringResource(Res.string.handoff_title), style = DraftDuelTheme.type.label, color = colors.muted)
                }
                Text(handoff.nextPlayer, style = DraftDuelTheme.type.hero, color = colors.accent, textAlign = TextAlign.Center)
                val card = handoff.card
                if (card != null) {
                    InfoPill {
                        Text(card.name, style = DraftDuelTheme.type.bodyStrong, color = colors.onScreen)
                        Text("${card.season} · ${card.country}", style = DraftDuelTheme.type.mono, color = colors.muted)
                    }
                }
                InfoPill {
                    val leader = handoff.leaderName
                    Text(
                        text = if (leader != null) {
                            stringResource(Res.string.handoff_leads_at, leader, handoff.price)
                        } else {
                            stringResource(Res.string.handoff_new_card)
                        },
                        style = DraftDuelTheme.type.mono,
                        color = colors.onScreen,
                    )
                }
            }
        }
        DdPrimaryButton(
            text = stringResource(Res.string.handoff_ready, handoff.nextPlayer),
            onClick = onReady,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(Res.string.handoff_hidden_note),
            style = DraftDuelTheme.type.mono,
            color = colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Two phones with an arrow from one to the other. */
@Composable
private fun HandoffPictogram() {
    val colors = DraftDuelTheme.colors
    Row(horizontalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(width = 150.dp, height = 100.dp)) {
            val stroke = 3.dp.toPx()
            val phone = Size(size.width * 0.28f, size.height * 0.72f)
            val top = size.height * 0.18f
            drawRoundRect(colors.muted, Offset(size.width * 0.1f, top), phone, CornerRadius(8.dp.toPx()), style = Stroke(stroke))
            drawRoundRect(colors.accent, Offset(size.width * 0.62f, top), phone, CornerRadius(8.dp.toPx()))
            val y = size.height * 0.1f
            drawLine(colors.onScreen, Offset(size.width * 0.38f, y + 6.dp.toPx()), Offset(size.width * 0.6f, y + 6.dp.toPx()), stroke, StrokeCap.Round)
            drawLine(colors.onScreen, Offset(size.width * 0.6f, y + 6.dp.toPx()), Offset(size.width * 0.55f, y), stroke, StrokeCap.Round)
            drawLine(colors.onScreen, Offset(size.width * 0.6f, y + 6.dp.toPx()), Offset(size.width * 0.55f, y + 12.dp.toPx()), stroke, StrokeCap.Round)
        }
    }
}

@Preview
@Composable
private fun HandoffScreenPreview() {
    HandoffScreen(
        handoff = HandoffUi(nextPlayer = "Ana", card = FakePacks.football.cards.first(), price = 9, leaderName = "Luka"),
        theme = Theme.FOOTBALL,
        onReady = {},
    )
}
