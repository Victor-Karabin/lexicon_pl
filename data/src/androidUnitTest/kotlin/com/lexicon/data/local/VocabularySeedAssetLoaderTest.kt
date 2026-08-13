package com.lexicon.data.local

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

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
        val assets = mockk<AssetReader> { every { readText("vocabulary_pl.json") } returns json }

        val words = VocabularySeedAssetLoader(assets).load()

        assertEquals(2, words.size)
        assertEquals(WordEntity(1, "kot", "cat", "kɔt", searchKey = "kot cat"), words[0])
        assertEquals(WordEntity(2, "pies", "dog", "pjɛs", searchKey = "pies dog"), words[1])
    }
}
