package com.vidanstojic.draftduel.platform

/**
 * Plays the game's audio files (composeResources/files/audio, made by tools/audio/make_audio.py).
 * One looping music track at a time and short sound effects on top. Android uses MediaPlayer and
 * SoundPool, iOS AVAudioPlayer. Names are file names without the extension.
 */
interface AudioOutput {
    /** Receives the bytes of every file once, before anything plays. */
    suspend fun load(files: Map<String, ByteArray>)

    /** Starts [name] looping, or keeps it going if it already plays. */
    fun playMusic(name: String, volume: Float)

    fun stopMusic()

    /** The app went to the background: music stops where it is, and resumes with [resumeMusic]. */
    fun pauseMusic()

    fun resumeMusic()

    fun playSound(name: String, volume: Float)
}

/** Haptic patterns, from the lightest to the strongest. */
enum class HapticEffect {
    /** Each of the last seconds of your turn. */
    TICK,

    /** It is your turn now: a short, soft buzz so you notice without looking. */
    YOUR_TURN,

    /** You won a card. */
    SUCCESS,

    /** Your time ran out. */
    WARNING,
}

interface HapticOutput {
    fun perform(effect: HapticEffect)
}

/** Previews, tests and platforms without sound. */
object SilentAudio : AudioOutput {
    override suspend fun load(files: Map<String, ByteArray>) = Unit
    override fun playMusic(name: String, volume: Float) = Unit
    override fun stopMusic() = Unit
    override fun pauseMusic() = Unit
    override fun resumeMusic() = Unit
    override fun playSound(name: String, volume: Float) = Unit
}

object NoHaptics : HapticOutput {
    override fun perform(effect: HapticEffect) = Unit
}
