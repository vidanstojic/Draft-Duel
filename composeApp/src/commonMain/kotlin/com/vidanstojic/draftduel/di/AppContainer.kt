package com.vidanstojic.draftduel.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.vidanstojic.draftduel.data.fake.FakeProfileRepository
import com.vidanstojic.draftduel.data.pack.BundledPackRepository
import com.vidanstojic.draftduel.domain.repository.InMemoryMatchSessionRepository
import com.vidanstojic.draftduel.domain.repository.MatchSessionRepository
import com.vidanstojic.draftduel.domain.repository.PackRepository
import com.vidanstojic.draftduel.domain.repository.ProfileRepository

/**
 * Manual dependency container, created once per process by the platform entry point
 * (DraftDuelApplication on Android, MainViewController on iOS).
 *
 * Swap the fake implementations for network/cache-backed ones here; screens only see the interfaces.
 */
class AppContainer(
    val appInfo: AppInfo = AppInfo.DEVELOPMENT,
    val features: Features = Features.forBuild(appInfo),
) {
    val packRepository: PackRepository = BundledPackRepository()
    val profileRepository: ProfileRepository = FakeProfileRepository()
    val sessionRepository: MatchSessionRepository = InMemoryMatchSessionRepository()
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not provided. Wrap the UI in App(container).")
}
