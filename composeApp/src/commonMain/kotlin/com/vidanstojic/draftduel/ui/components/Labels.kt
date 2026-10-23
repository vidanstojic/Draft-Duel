package com.vidanstojic.draftduel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme

/** Mono, letter-spaced caps label used for section names and small captions. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = DraftDuelTheme.colors.muted,
) {
    Text(text = text.uppercase(), style = DraftDuelTheme.type.label, color = color, modifier = modifier)
}

/** Rounded translucent pill that keeps text readable over any surface. */
@Composable
fun InfoPill(
    modifier: Modifier = Modifier,
    background: Color = DraftDuelTheme.colors.panel,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .background(background, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
