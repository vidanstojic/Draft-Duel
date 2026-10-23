package com.vidanstojic.draftduel.di

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Manual dependency container, created once per process by the platform entry point
 * (DraftDuelApplication on Android, MainViewController on iOS).
 */
class AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not provided. Wrap the UI in App(container).")
}
