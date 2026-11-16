package com.vidanstojic.draftduel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.vidanstojic.draftduel.di.AppContainer
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.navigation.DraftDuelNavHost
import com.vidanstojic.draftduel.resources.Res
import com.vidanstojic.draftduel.ui.components.DdScreen
import com.vidanstojic.draftduel.ui.surface.SurfaceStyle
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Root composable shared by Android and iOS. */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun App(container: AppContainer) {
    var packsLoaded by remember(container) { mutableStateOf(false) }
    LaunchedEffect(container) {
        container.packRepository.load()
        packsLoaded = true
    }
    // Audio loads on its own; the first screen does not wait for it.
    LaunchedEffect(container) {
        container.feedback.load { path -> Res.readBytes(path) }
    }
    // Music stops while the app is in the background and picks up where it was.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { container.feedback.onBackground() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { container.feedback.onForeground() }
    CompositionLocalProvider(LocalAppContainer provides container) {
        DraftDuelTheme {
            // Screens read packs synchronously, so they start once the bundled files are parsed (a few ms).
            if (packsLoaded) DraftDuelNavHost() else DdScreen(surface = SurfaceStyle.TABLE) {}
        }
    }
}
