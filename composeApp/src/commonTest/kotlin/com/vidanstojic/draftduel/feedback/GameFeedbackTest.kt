package com.vidanstojic.draftduel.feedback

import com.vidanstojic.draftduel.domain.model.Theme
import com.vidanstojic.draftduel.platform.AudioOutput
import com.vidanstojic.draftduel.platform.HapticEffect
import com.vidanstojic.draftduel.platform.HapticOutput
import com.vidanstojic.draftduel.platform.InMemoryKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameFeedbackTest {

    private class Recorder : AudioOutput, HapticOutput {
        val log = mutableListOf<String>()
        override suspend fun load(files: Map<String, ByteArray>) { log += "load ${files.size}" }
        override fun playMusic(name: String, volume: Float) { log += "music $name" }
        override fun stopMusic() { log += "stop" }
        override fun pauseMusic() { log += "pause" }
        override fun resumeMusic() { log += "resume" }
        override fun playSound(name: String, volume: Float) { log += "sound $name" }
        override fun perform(effect: HapticEffect) { log += "haptic $effect" }
    }

    private fun setup(): Triple<GameFeedback, SettingsRepository, Recorder> {
        val recorder = Recorder()
        val settings = SettingsRepository(InMemoryKeyValueStore())
        return Triple(GameFeedback(settings, recorder, recorder), settings, recorder)
    }

    @Test
    fun switches_silenceEachChannel() = runTest {
        val (feedback, settings, log) = setup()
        feedback.load { ByteArray(1) }
        feedback.play(Sfx.BID)
        feedback.vibrate(HapticEffect.TAP)
        settings.setSounds(false)
        settings.setVibration(false)
        feedback.play(Sfx.BID)
        feedback.vibrate(HapticEffect.TAP)
        assertEquals(listOf("load 14", "sound sfx_bid", "haptic TAP"), log.log)
    }

    @Test
    fun music_waitsForTheFiles_andFollowsTheSwitch() = runTest {
        val (feedback, settings, log) = setup()
        feedback.music(MusicTrack.forMatch(Theme.MOVIES))
        assertTrue(log.log.isEmpty(), "nothing plays before the files are loaded")
        feedback.load { ByteArray(1) }
        assertEquals("music music_movies", log.log.last())
        settings.setMusic(false)
        feedback.applyMusic()
        assertEquals("stop", log.log.last())
        settings.setMusic(true)
        feedback.music(MusicTrack.MENU)
        assertEquals("music music_menu", log.log.last())
    }

    @Test
    fun settings_areSaved() {
        val store = InMemoryKeyValueStore()
        SettingsRepository(store).setMusic(false)
        assertEquals(GameSettings(music = false), SettingsRepository(store).settings.value)
    }
}
