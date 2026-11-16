package com.vidanstojic.draftduel.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.vidanstojic.draftduel.data.fake.FakeProfileRepository
import com.vidanstojic.draftduel.data.pack.BundledPackRepository
import com.vidanstojic.draftduel.data.prefs.StoredSeenCards
import com.vidanstojic.draftduel.domain.model.ShuffleBag
import com.vidanstojic.draftduel.domain.repository.InMemoryMatchSessionRepository
import com.vidanstojic.draftduel.domain.repository.MatchSessionRepository
import com.vidanstojic.draftduel.domain.repository.PackRepository
import com.vidanstojic.draftduel.domain.repository.ProfileRepository
import com.vidanstojic.draftduel.feedback.GameFeedback
import com.vidanstojic.draftduel.feedback.SettingsRepository
import com.vidanstojic.draftduel.platform.PlatformServices

/**
 * Manual dependency container, created once per process by the platform entry point
 * (DraftDuelApplication on Android, MainViewController on iOS).
 *
 * Swap the fake implementations for network/cache-backed ones here; screens only see the interfaces.
 */
class AppContainer(
    val appInfo: AppInfo = AppInfo.DEVELOPMENT,
    val features: Features = Features.forBuild(appInfo),
    val platform: PlatformServices = PlatformServices.inMemory(),
) {
    val packRepository: PackRepository = BundledPackRepository(shuffleBag = ShuffleBag(StoredSeenCards(platform.store)))
    val profileRepository: ProfileRepository = FakeProfileRepository()
    val sessionRepository: MatchSessionRepository = InMemoryMatchSessionRepository()
    val settingsRepository: SettingsRepository = SettingsRepository(platform.store)
    val feedback: GameFeedback = GameFeedback(settingsRepository, platform.audio, platform.haptics)
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not provided. Wrap the UI in App(container).")
}
