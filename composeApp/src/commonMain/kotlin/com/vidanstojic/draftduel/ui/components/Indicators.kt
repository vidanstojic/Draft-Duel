package com.vidanstojic.draftduel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme

/** Ring that empties as the 10 s turn runs out, with the price in the middle. */
@Composable
fun TurnTimerRing(
    progress: Float,
    value: String,
    caption: String?,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
) {
    val colors = DraftDuelTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .background(colors.panel, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize().padding(4.dp)) {
            val stroke = 5.dp.toPx()
            drawCircle(colors.onScreen.copy(alpha = 0.18f), style = Stroke(stroke))
            drawArc(
                color = colors.accent,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = DraftDuelTheme.type.number, color = colors.onScreen)
            if (caption != null) {
                Text(caption, style = DraftDuelTheme.type.mono.copy(fontSize = 9.sp), color = colors.muted)
            }
        }
    }
}

/** Who leads the auction. Always arrow + text, never color alone (spec §14). */
@Composable
fun LeaderBadge(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = DraftDuelTheme.colors.opponent,
    content: Color = DraftDuelTheme.colors.onOpponent,
) {
    Row(
        modifier = modifier
            .background(container, CircleShape)
            .padding(start = 6.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(text.uppercase(), style = DraftDuelTheme.type.mono.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = content)
    }
}

/** 20 small bars showing the points left out of the starting budget. */
@Composable
fun BudgetMeter(
    points: Int,
    max: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val empty = DraftDuelTheme.colors.onScreen.copy(alpha = 0.2f)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(max) { index ->
            Box(
                Modifier
                    .size(width = 7.dp, height = 6.dp)
                    .background(if (index < points) color else empty, RoundedCornerShape(2.dp)),
            )
        }
    }
}

/** Decorative chip: filled disc with a dashed ring, as on real casino chips. */
@Composable
fun ChipDisc(
    color: Color,
    ringColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
) {
    Canvas(modifier.size(size)) {
        val ring = this.size.minDimension * 0.11f
        drawCircle(color)
        drawCircle(
            color = ringColor,
            radius = this.size.minDimension / 2 - ring,
            style = Stroke(width = ring, pathEffect = PathEffect.dashPathEffect(floatArrayOf(ring * 1.6f, ring))),
        )
    }
}
