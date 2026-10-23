package com.vidanstojic.draftduel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vidanstojic.draftduel.di.AppContainer
import com.vidanstojic.draftduel.di.LocalAppContainer

/** Root composable shared by Android and iOS. */
@Composable
fun App(container: AppContainer) {
    CompositionLocalProvider(LocalAppContainer provides container) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Draft Duel")
        }
    }
}
