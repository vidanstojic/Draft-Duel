package com.vidanstojic.draftduel.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

/** The iOS pieces behind [PlatformServices]. */
fun iosPlatformServices(): PlatformServices = PlatformServices(
    store = UserDefaultsStore(NSUserDefaults.standardUserDefaults),
    audio = IosAudio(),
    haptics = IosHaptics(),
)

private class UserDefaultsStore(private val defaults: NSUserDefaults) : KeyValueStore {

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(key) else defaults.setObject(value, forKey = key)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }
}

/**
 * AVAudioPlayer for music and effects. The ambient session category mixes with other apps and
 * respects the silent switch, as players expect from a casual game.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosAudio : AudioOutput {

    private val data = mutableMapOf<String, NSData>()
    private val effects = mutableMapOf<String, AVAudioPlayer>()
    private var music: AVAudioPlayer? = null
    private var playing: String? = null

    override suspend fun load(files: Map<String, ByteArray>) {
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryAmbient, error = null)
        files.forEach { (name, bytes) -> data[name] = bytes.toNSData() }
        data.filterKeys { it.startsWith("sfx_") }.forEach { (name, sound) ->
            AVAudioPlayer(data = sound, error = null).let { player ->
                player.prepareToPlay()
                effects[name] = player
            }
        }
    }

    override fun playMusic(name: String, volume: Float) {
        val current = music
        if (playing == name && current != null) {
            current.volume = volume
            if (!current.playing) current.play()
            return
        }
        stopMusic()
        val sound = data[name] ?: return
        music = AVAudioPlayer(data = sound, error = null).apply {
            numberOfLoops = -1
            this.volume = volume
            prepareToPlay()
            play()
        }
        playing = name
    }

    override fun stopMusic() {
        music?.stop()
        music = null
        playing = null
    }

    override fun pauseMusic() {
        music?.pause()
    }

    override fun resumeMusic() {
        music?.play()
    }

    override fun playSound(name: String, volume: Float) {
        val player = effects[name] ?: return
        player.currentTime = 0.0
        player.volume = volume
        player.play()
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}

private class IosHaptics : HapticOutput {
    override fun perform(effect: HapticEffect) {
        when (effect) {
            HapticEffect.TICK -> UISelectionFeedbackGenerator().selectionChanged()
            HapticEffect.TAP -> UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
            HapticEffect.SUCCESS -> UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            HapticEffect.WARNING -> UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
        }
    }
}
