package com.vidanstojic.draftduel.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vidanstojic.draftduel.ui.surface.SurfaceStyle
import com.vidanstojic.draftduel.ui.surface.surfaceScreenBackground
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme

/**
 * Root of every screen: applies the surface theme, paints the background edge to edge and keeps
 * content inside the safe area (status bar, notch, home indicator) on both platforms.
 */
@Composable
fun DdScreen(
    surface: SurfaceStyle,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DraftDuelTheme(surface = surface) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .surfaceScreenBackground(surface)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(DraftDuelTheme.spacing.screen),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}
