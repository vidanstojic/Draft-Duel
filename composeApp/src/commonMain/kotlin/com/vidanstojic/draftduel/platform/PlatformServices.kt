package com.vidanstojic.draftduel.platform

/**
 * What the shared code needs from the phone itself. Built once by the platform entry point
 * (DraftDuelApplication on Android, MainViewController on iOS) and handed to the AppContainer.
 */
class PlatformServices(
    val store: KeyValueStore,
    val audio: AudioOutput = SilentAudio,
    val haptics: HapticOutput = NoHaptics,
) {
    companion object {
        /** Previews and tests: nothing is saved, nothing is heard or felt. */
        fun inMemory(): PlatformServices = PlatformServices(store = InMemoryKeyValueStore())
    }
}
