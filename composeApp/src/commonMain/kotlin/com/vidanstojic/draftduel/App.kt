package com.vidanstojic.draftduel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vidanstojic.draftduel.di.AppContainer
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.navigation.DraftDuelNavHost
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.surface.SurfaceStyle
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme

/** Root composable shared by Android and iOS. */
@Composable
fun App(container: AppContainer) {
    var packsLoaded by remember(container) { mutableStateOf(false) }
    LaunchedEffect(container) {
        container.packRepository.load()
        packsLoaded = true
    }
    CompositionLocalProvider(LocalAppContainer provides container) {
        DraftDuelTheme {
            // Screens read packs synchronously, so they start once the bundled files are parsed (a few ms).
            if (packsLoaded) DraftDuelNavHost() else DdScreen(surface = SurfaceStyle.TABLE) {}
        }
    }
}
