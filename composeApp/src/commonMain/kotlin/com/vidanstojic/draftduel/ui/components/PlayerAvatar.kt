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
import androidx.compose.ui.geometry.CornerRadius
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
 * Generated card art, never a photo, poster or logo (spec §5): a faceless athlete in a numbered
 * jersey, a film frame for movies, a plate for ingredients. Colors come from a neutral colorway.
 */
@Composable
fun PlayerAvatar(
    spec: AvatarSpec,
    theme: Theme,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    when (theme) {
        Theme.FOOTBALL -> AthleteArt(spec, JerseyCut.SHIRT, contentDescription, modifier)
        Theme.TENNIS -> AthleteArt(spec, JerseyCut.POLO, contentDescription, modifier)
        Theme.BASKETBALL -> AthleteArt(spec, JerseyCut.TANK, contentDescription, modifier)
        Theme.MOVIES -> InitialsArt(spec, contentDescription, modifier) { colors -> drawFilmFrame(colors) }
        Theme.FOOD -> InitialsArt(spec, contentDescription, modifier) { colors -> drawPlate(colors) }
    }
}

private enum class JerseyCut { SHIRT, POLO, TANK }

@Composable
private fun AthleteArt(
    spec: AvatarSpec,
    cut: JerseyCut,
    contentDescription: String,
    modifier: Modifier,
) {
    val colors = spec.colorway.colors()
    BoxWithConstraints(
        modifier = modifier
            .background(colors.background)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Canvas(Modifier.fillMaxSize()) { drawSilhouette(cut, colors) }
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
private fun DrawScope.drawSilhouette(cut: JerseyCut, colors: AvatarColors) {
    val s = size.height / 112f
    val cx = size.width / 2f
    fun p(x: Float, y: Float) = Offset(cx + x * s, y * s)

    drawRoundRectNeck(cx, s)
    drawOval(HeadColor, topLeft = Offset(cx - 17 * s, 24 * s), size = Size(34 * s, 40 * s))

    val jersey = Path().apply {
        when (cut) {
            JerseyCut.TANK -> {
                moveTo(p(-48f, 112f).x, p(-48f, 112f).y)
                lineTo(p(-40f, 80f).x, p(-40f, 80f).y)
                cubicTo(p(-30f, 74f).x, p(-30f, 74f).y, p(-18f, 72f).x, p(-18f, 72f).y, p(-12f, 72f).x, p(-12f, 72f).y)
                lineTo(p(0f, 86f).x, p(0f, 86f).y)
                lineTo(p(12f, 72f).x, p(12f, 72f).y)
                cubicTo(p(18f, 72f).x, p(18f, 72f).y, p(30f, 74f).x, p(30f, 74f).y, p(40f, 80f).x, p(40f, 80f).y)
                lineTo(p(48f, 112f).x, p(48f, 112f).y)
            }
            JerseyCut.SHIRT, JerseyCut.POLO -> {
                moveTo(p(-56f, 112f).x, p(-56f, 112f).y)
                cubicTo(p(-52f, 84f).x, p(-52f, 84f).y, p(-30f, 72f).x, p(-30f, 72f).y, p(0f, 72f).x, p(0f, 72f).y)
                cubicTo(p(30f, 72f).x, p(30f, 72f).y, p(52f, 84f).x, p(52f, 84f).y, p(56f, 112f).x, p(56f, 112f).y)
            }
        }
        close()
    }
    drawPath(jersey, colors.jersey)

    if (cut != JerseyCut.TANK) {
        val collarDepth = if (cut == JerseyCut.POLO) 80f else 84f
        val collar = Path().apply {
            moveTo(p(-12f, 72f).x, p(-12f, 72f).y)
            lineTo(p(0f, collarDepth).x, p(0f, collarDepth).y)
            lineTo(p(12f, 72f).x, p(12f, 72f).y)
        }
        drawPath(collar, colors.number, style = Stroke(width = 4 * s, join = StrokeJoin.Round))
    }
}

private fun DrawScope.drawRoundRectNeck(cx: Float, s: Float) {
    drawRect(NeckColor, topLeft = Offset(cx - 8 * s, 58 * s), size = Size(16 * s, 14 * s))
}

/** Big initials over a drawn backdrop, for cards that are not athletes. */
@Composable
private fun InitialsArt(
    spec: AvatarSpec,
    contentDescription: String,
    modifier: Modifier,
    backdrop: DrawScope.(AvatarColors) -> Unit,
) {
    val colors = spec.colorway.colors()
    BoxWithConstraints(
        modifier = modifier
            .background(colors.background)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Canvas(Modifier.fillMaxSize()) { backdrop(colors) }
        val initialsSize = with(LocalDensity.current) { (maxHeight * 0.3f).toSp() }
        Text(
            text = spec.initials,
            color = colors.jersey,
            fontSize = initialsSize,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** A film frame: dark strip with sprocket holes along the top and bottom edges. */
private fun DrawScope.drawFilmFrame(colors: AvatarColors) {
    val strip = size.height * 0.16f
    val hole = strip * 0.45f
    for (top in listOf(0f, size.height - strip)) {
        drawRect(Color(0xFF15121A), Offset(0f, top), Size(size.width, strip))
        var x = hole * 0.6f
        while (x + hole < size.width) {
            drawRoundRect(
                color = colors.jersey.copy(alpha = 0.85f),
                topLeft = Offset(x, top + (strip - hole) / 2),
                size = Size(hole, hole),
                cornerRadius = CornerRadius(hole * 0.25f),
            )
            x += hole * 1.9f
        }
    }
    val frameInset = size.height * 0.06f
    drawRect(
        color = colors.jersey.copy(alpha = 0.5f),
        topLeft = Offset(frameInset, strip + frameInset),
        size = Size(size.width - frameInset * 2, size.height - (strip + frameInset) * 2),
        style = Stroke(width = size.height * 0.02f),
    )
}

/** A plate seen from above with a fork and a knife beside it. */
private fun DrawScope.drawPlate(colors: AvatarColors) {
    val radius = size.minDimension * 0.42f
    drawCircle(PlateColor, radius = radius, center = center)
    drawCircle(colors.background.copy(alpha = 0.25f), radius = radius * 0.72f, center = center, style = Stroke(width = radius * 0.04f))
    val cutlery = colors.jersey.copy(alpha = 0.9f)
    val width = size.minDimension * 0.035f
    val top = center.y - radius * 0.9f
    val bottom = center.y + radius * 0.9f
    val forkX = center.x - radius - width * 3
    val knifeX = center.x + radius + width * 3
    if (forkX > 0f && knifeX < size.width) {
        drawLine(cutlery, Offset(forkX, top), Offset(forkX, bottom), strokeWidth = width)
        for (tine in listOf(-1.5f, 0f, 1.5f)) {
            drawLine(cutlery, Offset(forkX + tine * width, top), Offset(forkX + tine * width, top + radius * 0.35f), strokeWidth = width * 0.6f)
        }
        drawLine(cutlery, Offset(knifeX, top), Offset(knifeX, bottom), strokeWidth = width * 1.6f)
    }
}

private val PlateColor = Color(0xFFF7F1E6)
