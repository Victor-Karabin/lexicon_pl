package com.lexicon.android.recognition

import android.util.Log
import com.lexicon.android.audio.AudioRecorder
import com.lexicon.android.cloud.CloudSpeechApi
import com.lexicon.android.cloud.CloudTranscript
import com.lexicon.boundary.SpeechRecognitionFailed
import com.lexicon.common.DispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CloudSpeechRecognizerServiceTest {
    private val dispatcher = StandardTestDispatcher()
    private val recording: File = File.createTempFile("pronunciation_attempt_", ".wav").apply { writeBytes(ByteArray(64)) }

    private val api: CloudSpeechApi = mockk { every { isConfigured } returns true }

    private val service = CloudSpeechRecognizerService(
        recorder = mockk<AudioRecorder> { coEvery { record() } returns recording.path },
        api = api,
        device = mockk(),
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
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun cleanUp() {
        unmockkStatic(Log::class)
        recording.delete()
    }

    private suspend fun failure(): SpeechRecognitionFailed? =
        try {
            service.recognize()
            null
        } catch (failure: SpeechRecognitionFailed) {
            failure
        }

    @Test
    fun `a recording the cloud could not transcribe is deleted`() =
        runTest(dispatcher) {
            every { api.recognize(any(), any(), any()) } returns null

            assertTrue(failure() != null)
            assertFalse(recording.exists())
        }

    @Test
    fun `a recording is deleted when the cloud call throws`() =
        runTest(dispatcher) {
            every { api.recognize(any(), any(), any()) } throws IllegalStateException("offline")

            assertTrue(failure() != null)
            assertFalse(recording.exists())
        }

    @Test
    fun `a transcribed recording is kept for playback`() =
        runTest(dispatcher) {
            every { api.recognize(any(), any(), any()) } returns CloudTranscript("dzień dobry", 0.9f)

            assertEquals(recording.path, service.recognize().audioFilePath)
            assertTrue(recording.exists())
        }
}
