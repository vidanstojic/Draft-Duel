package com.vidanstojic.draftduel.feedback

import com.vidanstojic.draftduel.platform.AudioOutput
import com.vidanstojic.draftduel.platform.HapticEffect
import com.vidanstojic.draftduel.platform.HapticOutput

/** Background music: one loop for the menus, one for every match ("Tactical Rounds"). */
enum class MusicTrack(val file: String) {
    MENU("music_menu"),
    MATCH("music_match"),
}

/** Short sounds, each with its own level so the buzzer does not drown the chip clack. */
enum class Sfx(val file: String, val volume: Float) {
    BID("sfx_bid", 0.9f),
    PASS("sfx_pass", 0.6f),
    CARD_WON("sfx_card_won", 0.7f),
    CARD_LOST("sfx_card_lost", 0.5f),
    TICK("sfx_tick", 0.45f),
    TIME_UP("sfx_time_up", 0.5f),
    WIN("sfx_win", 0.8f),
    LOSE("sfx_lose", 0.7f),
    DRAW("sfx_draw", 0.7f),
}

/**
 * Music, sounds and vibration in one place, each behind its switch in [SettingsRepository].
 * Screens and view models say what happened; this decides what the player hears and feels.
 */
class GameFeedback(
    private val settings: SettingsRepository,
    private val audio: AudioOutput,
    private val haptics: HapticOutput,
) {
    private var track: MusicTrack? = null
    private var loaded = false
    private var inBackground = false

    /** Reads the audio files ([readFile] gets a path under composeResources). */
    suspend fun load(readFile: suspend (String) -> ByteArray) {
        val names = MusicTrack.entries.map { it.file } + Sfx.entries.map { it.file }
        val files = names.mapNotNull { name -> runCatching { name to readFile("files/audio/$name.mp3") }.getOrNull() }.toMap()
        audio.load(files)
        loaded = true
        applyMusic()
    }

    /** The music that fits the current screen; it plays when music is on. */
    fun music(next: MusicTrack) {
        track = next
        applyMusic()
    }

    /** Call after the music switch changed. */
    fun applyMusic() {
        val current = track
        when {
            !loaded || inBackground || current == null -> Unit
            !settings.settings.value.music -> audio.stopMusic()
            else -> audio.playMusic(current.file, MUSIC_VOLUME)
        }
    }

    fun onBackground() {
        inBackground = true
        audio.pauseMusic()
    }

    fun onForeground() {
        inBackground = false
        if (settings.settings.value.music) audio.resumeMusic()
        applyMusic()
    }

    fun play(sfx: Sfx) {
        if (loaded && settings.settings.value.sounds) audio.playSound(sfx.file, sfx.volume)
    }

    fun vibrate(effect: HapticEffect) {
        if (settings.settings.value.vibration) haptics.perform(effect)
    }

    private companion object {
        /** Music stays under the effects. */
        const val MUSIC_VOLUME = 0.35f
    }
}
