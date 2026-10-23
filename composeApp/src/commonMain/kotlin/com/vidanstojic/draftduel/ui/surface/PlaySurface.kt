package com.vidanstojic.draftduel.ui.surface

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

/**
 * The surface seen from above with content laid out on it. Content is padded so it stays inside
 * the drawn area of the given [style].
 */
@Composable
fun PlaySurface(
    style: SurfaceStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 14.dp, shape = style.shape)
            .clip(style.shape),
    ) {
        Canvas(Modifier.matchParentSize()) { drawSurface(style) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(style.contentPadding),
            content = content,
        )
    }
}

/** Small, non-interactive preview of a surface (category tiles). */
@Composable
fun SurfacePreview(style: SurfaceStyle, modifier: Modifier = Modifier) {
    Canvas(modifier.clip(style.shape)) { drawSurface(style) }
}

/** Screen background around the surface: wood planks for the table, a flat color otherwise. */
fun Modifier.surfaceScreenBackground(style: SurfaceStyle): Modifier =
    if (style == SurfaceStyle.TABLE) {
        drawBehind { drawWoodFloor() }
    } else {
        background(style.palette.screen)
    }
