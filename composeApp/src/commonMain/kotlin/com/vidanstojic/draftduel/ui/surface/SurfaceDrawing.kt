package com.vidanstojic.draftduel.ui.surface

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/*
 * Top-down surfaces drawn in code, proportional to the canvas size, so the same function draws
 * the full match surface and the small previews on the category screen. No bitmaps (spec §14 size).
 */

private val LineWhite = Color(0xC7FFFFFF)

private fun DrawScope.lineWidth(): Float = max(1.dp.toPx(), size.minDimension * 0.0055f)

fun DrawScope.drawSurface(style: SurfaceStyle) = when (style) {
    SurfaceStyle.TABLE -> drawTable()
    SurfaceStyle.PITCH -> drawPitch()
    SurfaceStyle.COURT -> drawCourt()
    SurfaceStyle.CLAY -> drawClay()
    SurfaceStyle.CINEMA -> drawCinema()
    SurfaceStyle.KITCHEN -> drawKitchen()
}

/** Felt table with a wooden rim. */
fun DrawScope.drawTable() {
    val rimRadius = CornerRadius(min(size.width * 0.42f, size.height * 0.22f))
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF7A4A2A), Color(0xFF5A3219))),
        cornerRadius = rimRadius,
    )
    val inset = size.minDimension * 0.033f
    val feltSize = Size(size.width - inset * 2, size.height - inset * 2)
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF1F7558), Color(0xFF145238), Color(0xFF0E3D2A)),
            center = center,
            radius = max(feltSize.width, feltSize.height) * 0.62f,
        ),
        topLeft = Offset(inset, inset),
        size = feltSize,
        cornerRadius = CornerRadius(min(feltSize.width * 0.40f, feltSize.height * 0.20f)),
    )
}

/** Football pitch with mowing stripes, halfway line, center circle and both penalty areas. */
fun DrawScope.drawPitch() {
    val stripes = 12
    val stripeHeight = size.height / stripes
    repeat(stripes) { i ->
        drawRect(
            color = if (i % 2 == 0) Color(0xFF2F8F46) else Color(0xFF2A8340),
            topLeft = Offset(0f, i * stripeHeight),
            size = Size(size.width, stripeHeight + 1f),
        )
    }
    val stroke = Stroke(width = lineWidth())
    val inset = size.minDimension * 0.028f
    val field = Rect(inset, inset, size.width - inset, size.height - inset)
    drawRect(LineWhite, topLeft = field.topLeft, size = field.size, style = stroke)
    drawLine(LineWhite, Offset(field.left, center.y), Offset(field.right, center.y), strokeWidth = stroke.width)
    drawCircle(LineWhite, radius = size.minDimension * 0.17f, center = center, style = stroke)
    drawCircle(LineWhite, radius = stroke.width * 2f, center = center)

    val boxWidth = field.width * 0.64f
    val boxHeight = size.height * 0.18f
    val goalWidth = field.width * 0.32f
    val goalHeight = size.height * 0.065f
    val arcRadius = size.minDimension * 0.085f
    val spotDistance = boxHeight - arcRadius * 0.6f
    for (top in listOf(true, false)) {
        val edge = if (top) field.top else field.bottom
        val direction = if (top) 1f else -1f
        val boxTop = if (top) edge else edge - boxHeight
        val goalTop = if (top) edge else edge - goalHeight
        drawRect(LineWhite, Offset(center.x - boxWidth / 2, boxTop), Size(boxWidth, boxHeight), style = stroke)
        drawRect(LineWhite, Offset(center.x - goalWidth / 2, goalTop), Size(goalWidth, goalHeight), style = stroke)
        val spot = Offset(center.x, edge + direction * spotDistance)
        drawArc(
            color = LineWhite,
            startAngle = if (top) 36.87f else 216.87f,
            sweepAngle = 106.26f,
            useCenter = false,
            topLeft = Offset(spot.x - arcRadius, spot.y - arcRadius),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = stroke,
        )
    }
}

/** Basketball hardwood with painted keys, three-point lines and center circle. */
fun DrawScope.drawCourt() {
    val plank = size.minDimension * 0.075f
    val planks = listOf(Color(0xFFDCAE72), Color(0xFFD6A669), Color(0xFFE0B47A))
    var x = 0f
    var i = 0
    while (x < size.width) {
        drawRect(planks[i % planks.size], Offset(x, 0f), Size(plank, size.height))
        drawRect(Color(0xFFC99558), Offset(x + plank - 1f, 0f), Size(1f, size.height))
        x += plank
        i++
    }
    val line = Color(0xEBFFFFFF)
    val key = Color(0xFF1D4E9E)
    val stroke = Stroke(width = lineWidth())
    val inset = size.minDimension * 0.028f
    val court = Rect(inset, inset, size.width - inset, size.height - inset)
    drawRect(line, court.topLeft, court.size, style = stroke)
    drawLine(line, Offset(court.left, center.y), Offset(court.right, center.y), strokeWidth = stroke.width)
    val centerRadius = size.minDimension * 0.155f
    drawCircle(key.copy(alpha = 0.35f), radius = centerRadius, center = center)
    drawCircle(line, radius = centerRadius, center = center, style = stroke)

    val keyWidth = size.width * 0.34f
    val keyHeight = size.height * 0.2f
    val freeThrowRadius = keyWidth * 0.33f
    val arcWidth = court.width * 0.9f
    val arcHeight = size.height * 0.5f
    for (top in listOf(true, false)) {
        val edge = if (top) court.top else court.bottom
        val keyTop = if (top) edge else edge - keyHeight
        drawRect(key, Offset(center.x - keyWidth / 2, keyTop), Size(keyWidth, keyHeight))
        drawRect(line, Offset(center.x - keyWidth / 2, keyTop), Size(keyWidth, keyHeight), style = stroke)
        val freeThrowY = if (top) edge + keyHeight else edge - keyHeight
        drawArc(
            color = line,
            startAngle = if (top) 0f else 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - freeThrowRadius, freeThrowY - freeThrowRadius),
            size = Size(freeThrowRadius * 2, freeThrowRadius * 2),
            style = stroke,
        )
        drawArc(
            color = line,
            startAngle = if (top) 0f else 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - arcWidth / 2, edge - arcHeight / 2),
            size = Size(arcWidth, arcHeight),
            style = stroke,
        )
    }
}

/** Clay tennis court: doubles and singles lines, service boxes and a dashed net. */
fun DrawScope.drawClay() {
    drawRect(Color(0xFFC8643B))
    val stroke = Stroke(width = lineWidth())
    val insetX = size.width * 0.07f
    val insetY = size.minDimension * 0.028f
    val outer = Rect(insetX, insetY, size.width - insetX, size.height - insetY)
    drawRect(LineWhite, outer.topLeft, outer.size, style = stroke)
    val singles = outer.width * 0.125f
    drawLine(LineWhite, Offset(outer.left + singles, outer.top), Offset(outer.left + singles, outer.bottom), stroke.width)
    drawLine(LineWhite, Offset(outer.right - singles, outer.top), Offset(outer.right - singles, outer.bottom), stroke.width)
    val serviceTop = outer.top + outer.height * 0.27f
    val serviceBottom = outer.bottom - outer.height * 0.27f
    drawLine(LineWhite, Offset(outer.left + singles, serviceTop), Offset(outer.right - singles, serviceTop), stroke.width)
    drawLine(LineWhite, Offset(outer.left + singles, serviceBottom), Offset(outer.right - singles, serviceBottom), stroke.width)
    drawLine(LineWhite, Offset(center.x, serviceTop), Offset(center.x, serviceBottom), stroke.width)
    drawLine(
        color = Color.White,
        start = Offset(outer.left - insetX * 0.4f, center.y),
        end = Offset(outer.right + insetX * 0.4f, center.y),
        strokeWidth = stroke.width * 1.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke.width * 3, stroke.width * 2)),
    )
}

/** Velvet curtains with a gold valance, a glowing screen in the middle and rows of seats below. */
fun DrawScope.drawCinema() {
    val folds = 14
    val foldWidth = size.width / folds
    repeat(folds) { i ->
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF4A0C15), Color(0xFF7A1626), Color(0xFF4A0C15)),
                startX = i * foldWidth,
                endX = (i + 1) * foldWidth,
            ),
            topLeft = Offset(i * foldWidth, 0f),
            size = Size(foldWidth + 1f, size.height),
        )
    }

    val screen = Rect(size.width * 0.12f, size.height * 0.1f, size.width * 0.88f, size.height * 0.72f)
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF3A3F55), Color(0xFF1B1D29)),
            center = screen.center,
            radius = max(screen.width, screen.height) * 0.7f,
        ),
        topLeft = screen.topLeft,
        size = screen.size,
        cornerRadius = CornerRadius(size.minDimension * 0.02f),
    )
    // Film perforations down both sides of the screen.
    val hole = size.minDimension * 0.022f
    val gap = hole * 2.2f
    var y = screen.top + gap / 2
    while (y + hole < screen.bottom) {
        for (x in listOf(screen.left + hole * 0.6f, screen.right - hole * 1.6f)) {
            drawRoundRect(Color(0x33FFFFFF), Offset(x, y), Size(hole, hole * 0.7f), CornerRadius(hole * 0.2f))
        }
        y += gap
    }

    val gold = Color(0xFFC9973A)
    val valance = size.height * 0.06f
    drawRect(Color(0xFF6A1220), Offset.Zero, Size(size.width, valance))
    val scallops = 9
    val scallopWidth = size.width / scallops
    repeat(scallops) { i ->
        drawArc(
            color = Color(0xFF6A1220),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(i * scallopWidth, valance - scallopWidth / 2),
            size = Size(scallopWidth, scallopWidth),
        )
        drawArc(
            color = gold,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(i * scallopWidth, valance - scallopWidth / 2),
            size = Size(scallopWidth, scallopWidth),
            style = Stroke(width = lineWidth()),
        )
    }

    val seatRows = 3
    val seatsPerRow = 9
    val seatWidth = size.width / (seatsPerRow + 1)
    repeat(seatRows) { row ->
        val rowTop = size.height * (0.78f + row * 0.07f)
        repeat(seatsPerRow) { seat ->
            val left = seatWidth * (seat + 0.5f) + if (row % 2 == 1) seatWidth / 2 else 0f
            if (left + seatWidth * 0.8f <= size.width) {
                drawRoundRect(
                    color = Color(0xFF2A060C),
                    topLeft = Offset(left, rowTop),
                    size = Size(seatWidth * 0.8f, size.height * 0.05f),
                    cornerRadius = CornerRadius(seatWidth * 0.3f),
                )
            }
        }
    }
}

/** A wooden kitchen table under a gingham cloth, with a plate outline where the card sits. */
fun DrawScope.drawKitchen() {
    val plank = size.minDimension * 0.11f
    val woods = listOf(Color(0xFF6B4329), Color(0xFF61391F), Color(0xFF704A2E))
    var top = 0f
    var i = 0
    while (top < size.height) {
        drawRect(woods[i % woods.size], Offset(0f, top), Size(size.width, plank))
        drawRect(Color(0xFF4A2B17), Offset(0f, top + plank - 1f), Size(size.width, 1f))
        top += plank
        i++
    }

    val inset = size.minDimension * 0.05f
    val cloth = Rect(inset, inset, size.width - inset, size.height - inset)
    drawRect(Color(0xFF7E2620), cloth.topLeft, cloth.size)
    val check = size.minDimension * 0.07f
    val band = Color(0x40F7E6D0)
    var x = cloth.left
    var column = 0
    while (x < cloth.right) {
        if (column % 2 == 0) drawRect(band, Offset(x, cloth.top), Size(min(check, cloth.right - x), cloth.height))
        x += check
        column++
    }
    var y = cloth.top
    var row = 0
    while (y < cloth.bottom) {
        if (row % 2 == 0) drawRect(band, Offset(cloth.left, y), Size(cloth.width, min(check, cloth.bottom - y)))
        y += check
        row++
    }
    drawRect(Color(0x66000000), cloth.topLeft, cloth.size, style = Stroke(width = lineWidth() * 2))

    val plateRadius = size.minDimension * 0.3f
    drawCircle(Color(0x33FFFFFF), radius = plateRadius, center = center)
    drawCircle(Color(0x55FFFFFF), radius = plateRadius, center = center, style = Stroke(width = lineWidth() * 1.5f))
    drawCircle(Color(0x33FFFFFF), radius = plateRadius * 0.72f, center = center, style = Stroke(width = lineWidth()))
}

/** Vertical wood planks behind the table, as on the menu screens. */
fun DrawScope.drawWoodFloor() {
    val plank = 48.dp.toPx()
    val seam = 2.dp.toPx()
    drawRect(Color(0xFF24150E))
    var x = 0f
    while (x < size.width) {
        drawRect(Color(0xFF2B1A12), Offset(x, 0f), Size(plank - seam, size.height))
        x += plank
    }
}
