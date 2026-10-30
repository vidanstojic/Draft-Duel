package com.vidanstojic.draftduel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.vidanstojic.draftduel.domain.model.AvatarColorway
import com.vidanstojic.draftduel.domain.model.AvatarSpec
import com.vidanstojic.draftduel.domain.model.Theme

private data class AvatarColors(val background: Color, val jersey: Color, val number: Color)

private fun AvatarColorway.colors(): AvatarColors = when (this) {
    AvatarColorway.NAVY_GOLD -> AvatarColors(Color(0xFF24387A), Color(0xFFF7D774), Color(0xFF1C2A5A))
    AvatarColorway.TEAL_ORANGE -> AvatarColors(Color(0xFF1F5E5A), Color(0xFFE8743B), Color(0xFF1F2A2A))
    AvatarColorway.PLUM_CREAM -> AvatarColors(Color(0xFF4A2A4F), Color(0xFFF4ECD6), Color(0xFF4A2A4F))
    AvatarColorway.FOREST_SAND -> AvatarColors(Color(0xFF24483A), Color(0xFFD9C48F), Color(0xFF24483A))
    AvatarColorway.SLATE_LIME -> AvatarColors(Color(0xFF33404D), Color(0xFFB9E36A), Color(0xFF33404D))
}

private val HeadColor = Color(0xFFF4ECD6)
private val NeckColor = Color(0xFFE9DFC4)

/**
 * Faceless silhouette in a numbered jersey (spec §5: no photos, no likeness). The jersey cut
 * follows the theme; colors come from a neutral colorway, never a real club's.
 */
@Composable
fun PlayerAvatar(
    spec: AvatarSpec,
    theme: Theme,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = spec.colorway.colors()
    BoxWithConstraints(
        modifier = modifier
            .background(colors.background)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Canvas(Modifier.fillMaxSize()) { drawSilhouette(theme, colors) }
        val numberSize = with(LocalDensity.current) { (maxHeight * 0.2f).toSp() }
        Text(
            text = spec.jerseyNumber.toString(),
            color = colors.number,
            fontSize = numberSize,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = maxHeight * 0.03f),
        )
    }
}

/** Drawn in a 140 x 112 design space scaled to the height and centered horizontally. */
private fun DrawScope.drawSilhouette(theme: Theme, colors: AvatarColors) {
    val s = size.height / 112f
    val cx = size.width / 2f
    fun p(x: Float, y: Float) = Offset(cx + x * s, y * s)

    drawRoundRectNeck(cx, s)
    drawOval(HeadColor, topLeft = Offset(cx - 17 * s, 24 * s), size = Size(34 * s, 40 * s))

    val jersey = Path().apply {
        when (theme) {
            Theme.BASKETBALL -> {
                moveTo(p(-48f, 112f).x, p(-48f, 112f).y)
                lineTo(p(-40f, 80f).x, p(-40f, 80f).y)
                cubicTo(p(-30f, 74f).x, p(-30f, 74f).y, p(-18f, 72f).x, p(-18f, 72f).y, p(-12f, 72f).x, p(-12f, 72f).y)
                lineTo(p(0f, 86f).x, p(0f, 86f).y)
                lineTo(p(12f, 72f).x, p(12f, 72f).y)
                cubicTo(p(18f, 72f).x, p(18f, 72f).y, p(30f, 74f).x, p(30f, 74f).y, p(40f, 80f).x, p(40f, 80f).y)
                lineTo(p(48f, 112f).x, p(48f, 112f).y)
            }
            Theme.FOOTBALL, Theme.TENNIS -> {
                moveTo(p(-56f, 112f).x, p(-56f, 112f).y)
                cubicTo(p(-52f, 84f).x, p(-52f, 84f).y, p(-30f, 72f).x, p(-30f, 72f).y, p(0f, 72f).x, p(0f, 72f).y)
                cubicTo(p(30f, 72f).x, p(30f, 72f).y, p(52f, 84f).x, p(52f, 84f).y, p(56f, 112f).x, p(56f, 112f).y)
            }
        }
        close()
    }
    drawPath(jersey, colors.jersey)

    if (theme != Theme.BASKETBALL) {
        val collar = Path().apply {
            moveTo(p(-12f, 72f).x, p(-12f, 72f).y)
            lineTo(p(0f, if (theme == Theme.TENNIS) 80f else 84f).x, p(0f, if (theme == Theme.TENNIS) 80f else 84f).y)
            lineTo(p(12f, 72f).x, p(12f, 72f).y)
        }
        drawPath(collar, colors.number, style = Stroke(width = 4 * s, join = StrokeJoin.Round))
    }
}

private fun DrawScope.drawRoundRectNeck(cx: Float, s: Float) {
    drawRect(NeckColor, topLeft = Offset(cx - 8 * s, 58 * s), size = Size(16 * s, 14 * s))
}
