package com.lexicon.android.speech

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AndroidSpeechStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val store by lazy { AndroidSpeechStore(folder.root) }

    @Test
    fun `stored audio is found again with every byte`() {
        val audio = ByteArray(4096) { it.toByte() }

        val path = store.store(voice = "pl-PL-Wavenet-A", text = "dzień dobry", audio = audio)

        assertEquals(path, store.filePath(voice = "pl-PL-Wavenet-A", text = "dzień dobry"))
        assertArrayEquals(audio, File(checkNotNull(path)).readBytes())
    }

    @Test
    fun `storing leaves only the finished audio behind`() {
        store.store(voice = "pl-PL-Wavenet-A", text = "woda", audio = byteArrayOf(1, 2, 3))

        val files = folder.root.walkTopDown().filter { it.isFile }.toList()

        assertEquals(1, files.size)
        assertTrue(files.single().name.endsWith(".mp3"))
    }

    @Test
    fun `a newer recording replaces the cached one`() {
        store.store(voice = "pl-PL-Wavenet-A", text = "woda", audio = byteArrayOf(1, 2, 3))

        val path = store.store(voice = "pl-PL-Wavenet-A", text = "woda", audio = byteArrayOf(9))

        assertArrayEquals(byteArrayOf(9), File(checkNotNull(path)).readBytes())
    }

    @Test
    fun `text never synthesised has nothing cached`() {
        assertNull(store.filePath(voice = "pl-PL-Wavenet-A", text = "chleb"))
    }
}
