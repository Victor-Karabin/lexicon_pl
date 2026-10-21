package com.lexicon.android.lesson

import android.content.Context
import android.util.Log
import com.lexicon.common.DispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class AndroidLessonAudioLibraryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
    }

    private var logged = 0

    @Before
    fun silenceLog() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } answers { logged++ }
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } answers { logged++ }
        every { Log.w(any<String>(), any<Throwable>()) } answers { logged++ }
        every { Log.e(any<String>(), any<String>()) } answers { logged++ }
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } answers { logged++ }
    }

    @After
    fun restoreLog() = unmockkStatic(Log::class)

    private fun library(respond: (okhttp3.Request) -> Response): AndroidLessonAudioLibrary {
        val context = mockk<Context> { every { getExternalFilesDir(any()) } returns folder.root }
        val client = OkHttpClient.Builder().addInterceptor { chain -> respond(chain.request()) }.build()
        return AndroidLessonAudioLibrary(context, client, dispatchers)
    }

    private fun okhttp3.Request.reply(
        code: Int,
        type: String,
        body: ByteArray,
    ): Response =
        Response.Builder()
            .request(this)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .body(body.toResponseBody(type.toMediaType()))
            .build()

    private fun downloaded() = folder.root.walkTopDown().filter { it.isFile }.toList()

    @Test
    fun `an audio response is saved and its path returned`() =
        runTest {
            val audio = ByteArray(2048) { it.toByte() }
            val library = library { it.reply(200, "audio/mpeg", audio) }

            val path = library.pathOrNull("1/track.mp3", remoteId = "abc")

            assertArrayEquals(audio, File(checkNotNull(path)).readBytes())
        }

    @Test
    fun `a web page served with status 200 is not kept as the audio file`() =
        runTest {
            val library = library { it.reply(200, "text/html; charset=utf-8", "<html>Virus scan warning</html>".toByteArray()) }

            assertNull(library.pathOrNull("1/track.mp3", remoteId = "abc"))
            assertTrue("nothing is left behind: ${downloaded()}", downloaded().isEmpty())
            assertNull(library.localPathOrNull("1/track.mp3"))
        }

    @Test
    fun `a failed download is logged and leaves no file`() =
        runTest {
            val library = library { throw IOException("offline") }

            assertNull(library.pathOrNull("1/track.mp3", remoteId = "abc"))
            assertTrue("the failure should be logged", logged > 0)
            assertTrue(downloaded().isEmpty())
        }

    @Test
    fun `an error status is logged and leaves no file`() =
        runTest {
            val library = library { it.reply(404, "text/html", "not found".toByteArray()) }

            assertNull(library.pathOrNull("1/track.mp3", remoteId = "abc"))
            assertTrue("the failure should be logged", logged > 0)
            assertTrue(downloaded().isEmpty())
        }

    @Test
    fun `cancelling a download cancels the caller instead of being swallowed`() =
        runTest {
            val library = library { throw CancellationException("cancelled") }

            try {
                library.pathOrNull("1/track.mp3", remoteId = "abc")
                fail("the cancellation was swallowed")
            } catch (expected: CancellationException) {
                assertEquals("cancelled", expected.message)
            }
            assertFalse(downloaded().any { it.name.endsWith(".part") })
        }
}
