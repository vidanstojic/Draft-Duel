package com.vidanstojic.draftduel

import androidx.compose.ui.window.ComposeUIViewController
import com.vidanstojic.draftduel.di.AppContainer
import com.vidanstojic.draftduel.platform.iosPlatformServices
import platform.UIKit.UIViewController

/** App-scoped dependencies for the iOS process (the iOS counterpart of DraftDuelApplication). */
private val appContainer: AppContainer by lazy { AppContainer(platform = iosPlatformServices()) }

/** Entry point called from Swift: `MainViewControllerKt.MainViewController()`. */
fun MainViewController(): UIViewController = ComposeUIViewController {
    App(container = appContainer)
}
