package com.vidanstojic.draftduel.feedback

import com.vidanstojic.draftduel.platform.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Player settings, all on by default. */
data class GameSettings(
    val music: Boolean = true,
    val sounds: Boolean = true,
    val vibration: Boolean = true,
)

/** Reads and saves [GameSettings] in the phone's key-value store. */
class SettingsRepository(private val store: KeyValueStore) {

    private val _settings = MutableStateFlow(
        GameSettings(
            music = store.getBoolean(MUSIC, true),
            sounds = store.getBoolean(SOUNDS, true),
            vibration = store.getBoolean(VIBRATION, true),
        ),
    )
    val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    fun setMusic(on: Boolean) = update(MUSIC, on) { it.copy(music = on) }

    fun setSounds(on: Boolean) = update(SOUNDS, on) { it.copy(sounds = on) }

    fun setVibration(on: Boolean) = update(VIBRATION, on) { it.copy(vibration = on) }

    private fun update(key: String, value: Boolean, change: (GameSettings) -> GameSettings) {
        store.putBoolean(key, value)
        _settings.value = change(_settings.value)
    }

    private companion object {
        const val MUSIC = "settings.music"
        const val SOUNDS = "settings.sounds"
        const val VIBRATION = "settings.vibration"
    }
}
