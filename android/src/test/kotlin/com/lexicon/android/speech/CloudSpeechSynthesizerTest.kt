package com.lexicon.android.speech

import android.util.Log
import com.lexicon.android.cloud.CloudSpeechApi
import com.lexicon.boundary.AudioPlayer
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.boundary.SpeechVoice
import com.lexicon.common.DispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CloudSpeechSynthesizerTest {
    private class SlowVoice : SpeechSynthesizer {
        val spoken = mutableListOf<String>()
        val finish = CompletableDeferred<Unit>()

        override suspend fun speak(text: String) {
            spoken += text
            finish.await()
        }

        override suspend fun voices(): List<SpeechVoice> = emptyList()
    }

    private val dispatcher = StandardTestDispatcher()
    private val voice = SlowVoice()

    private val synthesizer = CloudSpeechSynthesizer(
        api = CloudSpeechApi(apiKey = ""),
        store = mockk(relaxed = true),
        player = mockk<AudioPlayer>(relaxed = true),
        settings = VoicePreference { null },
        fallback = voice,
        dispatchers = object : DispatcherProvider {
            override val main: CoroutineDispatcher = dispatcher
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
        },
    )

    @Before
    fun quietLog() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun restoreLog() = unmockkStatic(Log::class)

    @Test
    fun `taps while a word is still playing are ignored`() =
        runTest(dispatcher) {
            repeat(5) { launch { synthesizer.speak("dwanaście") } }
            advanceUntilIdle()
            assertEquals(listOf("dwanaście"), voice.spoken)

            voice.finish.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun `the next tap after playing ends is heard`() =
        runTest(dispatcher) {
            voice.finish.complete(Unit)
            synthesizer.speak("jeden")
            synthesizer.speak("dwa")
            assertEquals(listOf("jeden", "dwa"), voice.spoken)
        }
}
