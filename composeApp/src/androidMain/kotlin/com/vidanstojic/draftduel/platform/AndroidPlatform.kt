package com.vidanstojic.draftduel.platform

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** The Android pieces behind [PlatformServices]. */
fun androidPlatformServices(context: Context): PlatformServices = PlatformServices(
    store = SharedPreferencesStore(context.getSharedPreferences("draft_duel", Context.MODE_PRIVATE)),
    audio = AndroidAudio(context.applicationContext),
    haptics = AndroidHaptics(context.applicationContext),
)

private class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}

/**
 * MediaPlayer for the looping music, SoundPool for the short effects. Both need a file path, so
 * the bundled MP3s are copied to the cache folder once.
 */
private class AndroidAudio(private val context: Context) : AudioOutput {

    private val paths = mutableMapOf<String, String>()
    private val soundIds = mutableMapOf<String, Int>()
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(attributes(AudioAttributes.CONTENT_TYPE_SONIFICATION))
        .build()

    private var player: MediaPlayer? = null
    private var playing: String? = null

    override suspend fun load(files: Map<String, ByteArray>) {
        val written = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "audio").apply { mkdirs() }
            files.mapValues { (name, bytes) ->
                val file = File(dir, "$name.mp3")
                if (!file.exists() || file.length() != bytes.size.toLong()) file.writeBytes(bytes)
                file.path
            }
        }
        paths.putAll(written)
        written.filterKeys { it.startsWith("sfx_") }.forEach { (name, path) -> soundIds[name] = soundPool.load(path, 1) }
    }

    override fun playMusic(name: String, volume: Float) {
        val current = player
        if (playing == name && current != null) {
            current.setVolume(volume, volume)
            runCatching { if (!current.isPlaying) current.start() }
            return
        }
        stopMusic()
        val path = paths[name] ?: return
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(attributes(AudioAttributes.CONTENT_TYPE_MUSIC))
                setDataSource(path)
                isLooping = true
                setVolume(volume, volume)
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        }.getOrNull()
        playing = name.takeIf { player != null }
    }

    override fun stopMusic() {
        player?.release()
        player = null
        playing = null
    }

    override fun pauseMusic() {
        runCatching { player?.takeIf { it.isPlaying }?.pause() }
    }

    override fun resumeMusic() {
        runCatching { player?.start() }
    }

    override fun playSound(name: String, volume: Float) {
        val id = soundIds[name] ?: return
        soundPool.play(id, volume, volume, 1, 0, 1f)
    }

    private fun attributes(contentType: Int): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(contentType)
        .build()

    private companion object {
        const val MAX_STREAMS = 6
    }
}

/** Short one-shots and two-pulse patterns; amplitudes only where the motor supports them. */
private class AndroidHaptics(context: Context) : HapticOutput {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    override fun perform(effect: HapticEffect) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        val amplitudes = vibrator.hasAmplitudeControl()
        fun shot(millis: Long, amplitude: Int) =
            VibrationEffect.createOneShot(millis, if (amplitudes) amplitude else VibrationEffect.DEFAULT_AMPLITUDE)
        fun pattern(timings: LongArray, levels: IntArray) =
            if (amplitudes) VibrationEffect.createWaveform(timings, levels, -1) else VibrationEffect.createWaveform(timings, -1)
        val vibration = when (effect) {
            HapticEffect.TICK -> shot(12, 70)
            HapticEffect.YOUR_TURN -> shot(22, 90)
            HapticEffect.SUCCESS -> pattern(longArrayOf(0, 25, 70, 40), intArrayOf(0, 140, 0, 220))
            HapticEffect.WARNING -> pattern(longArrayOf(0, 90, 80, 160), intArrayOf(0, 255, 0, 255))
        }
        runCatching { vibrator.vibrate(vibration) }
    }
}
