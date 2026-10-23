package com.vidanstojic.draftduel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.vidanstojic.draftduel.di.AppContainer
import com.vidanstojic.draftduel.di.LocalAppContainer
import com.vidanstojic.draftduel.navigation.DraftDuelNavHost
import com.vidanstojic.draftduel.ui.theme.DraftDuelTheme

/** Root composable shared by Android and iOS. */
@Composable
fun App(container: AppContainer) {
    CompositionLocalProvider(LocalAppContainer provides container) {
        DraftDuelTheme {
            DraftDuelNavHost()
        }
    }
}
