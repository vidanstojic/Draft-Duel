package com.vidanstojic.draftduel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.vidanstojic.draftduel.ui.surface.SurfaceStyle

private val LocalSurfaceStyle = staticCompositionLocalOf { SurfaceStyle.TABLE }
private val LocalTypography = staticCompositionLocalOf { DraftDuelTypography() }
private val LocalSpacing = staticCompositionLocalOf { Spacing() }

/** Access point for Draft Duel design tokens, mirroring `MaterialTheme.colorScheme` style. */
object DraftDuelTheme {
    val surface: SurfaceStyle
        @Composable @ReadOnlyComposable get() = LocalSurfaceStyle.current

    val colors: SurfacePalette
        @Composable @ReadOnlyComposable get() = LocalSurfaceStyle.current.palette

    val type: DraftDuelTypography
        @Composable @ReadOnlyComposable get() = LocalTypography.current

    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
}

/**
 * Applies the tokens of [surface]. Screens nest this with the sport's surface, so the same
 * components automatically recolor for the table, pitch, court or clay.
 */
@Composable
fun DraftDuelTheme(
    surface: SurfaceStyle = SurfaceStyle.TABLE,
    content: @Composable () -> Unit,
) {
    val palette = surface.palette
    CompositionLocalProvider(LocalSurfaceStyle provides surface) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = palette.accent,
                onPrimary = palette.onAccent,
                background = palette.screen,
                onBackground = palette.onScreen,
                surface = palette.screen,
                onSurface = palette.onScreen,
                onSurfaceVariant = palette.muted,
                outline = palette.outline,
            ),
            content = content,
        )
    }
}
