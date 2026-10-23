package com.vidanstojic.draftduel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vidanstojic.draftduel.ui.theme.BidColors
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme

/** A pressable face sitting on a darker edge, like a casino chip or a physical button. */
@Composable
fun EdgeSurface(
    colors: BidColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    edge: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = edge)
                .background(colors.edge, shape),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = edge)
                .clip(shape)
                .background(colors.container)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.content) {
                content()
            }
        }
    }
}

@Composable
fun DdPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = DraftDuelTheme.colors
    EdgeSurface(
        colors = BidColors(colors.accent, colors.onAccent, colors.accentEdge),
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(64.dp)
            .alpha(if (enabled) 1f else 0.45f),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text, style = DraftDuelTheme.type.title)
        }
    }
}

@Composable
fun DdOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 50.dp,
) {
    val colors = DraftDuelTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(height)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(colors.panel)
            .border(1.5.dp, colors.outline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = DraftDuelTheme.type.bodyStrong, color = colors.onScreen)
    }
}

@Composable
fun DdIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DraftDuelTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(DraftDuelTheme.spacing.touch)
            .clip(shape)
            .background(colors.panel)
            .border(1.5.dp, colors.outline, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = colors.onScreen, modifier = Modifier.size(20.dp))
    }
}
