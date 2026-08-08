package com.lexicon.data.local

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class VocabularySeedAssetLoaderTest {
    @Test
    fun `parses seed items from the asset JSON into WordEntity rows`() {
        val json =
            """
            [
              {"id": 1, "text": "kot", "translation": "cat", "transcription": "kɔt"},
              {"id": 2, "text": "pies", "translation": "dog", "transcription": "pjɛs"}
            ]
            """.trimIndent()
        val assetManager = mockk<AssetManager> { every { open("vocabulary_pl.json") } returns ByteArrayInputStream(json.toByteArray()) }
        val context = mockk<Context> { every { assets } returns assetManager }

        val words = VocabularySeedAssetLoader(context).load()

        assertEquals(2, words.size)
        assertEquals(WordEntity(1, "kot", "cat", "kɔt", searchKey = "kot cat"), words[0])
        assertEquals(WordEntity(2, "pies", "dog", "pjɛs", searchKey = "pies dog"), words[1])
    }
}
