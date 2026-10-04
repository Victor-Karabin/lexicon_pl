package com.lexicon.data.local

import com.lexicon.model.vocabulary.Gender
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.WordForms
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun `a noun keeps its gender and plural, an adjective its five forms`() {
        val json =
            """
            [
              {"id": 1, "text": "kot", "translation": "cat", "transcription": "kɔt", "partOfSpeech": "n",
               "gender": "masculine animate", "plural": "koty"},
              {"id": 2, "text": "dobry", "translation": "good", "transcription": "", "partOfSpeech": "adj",
               "forms": ["dobry", "dobra", "dobre", "dobrzy", "dobre"]}
            ]
            """.trimIndent()
        val assets = mockk<AssetReader> { every { readText("vocabulary_pl.json") } returns json }

        val words = VocabularySeedAssetLoader(assets).load().map { it.toWord() }

        assertEquals(PartOfSpeech.NOUN, words[0].partOfSpeech)
        assertEquals(WordForms.Noun(Gender.MASCULINE_ANIMATE, "koty"), words[0].forms)
        assertEquals(PartOfSpeech.ADJECTIVE, words[1].partOfSpeech)
        assertEquals(WordForms.Adjective("dobry", "dobra", "dobre", "dobrzy", "dobre"), words[1].forms)
    }

    @Test
    fun `a word without grammar has none, and an unknown part of speech is dropped`() {
        val json =
            """
            [
              {"id": 1, "text": "bez", "translation": "without", "transcription": "", "partOfSpeech": "prep"},
              {"id": 2, "text": "coś", "translation": "something", "transcription": "", "partOfSpeech": "???"}
            ]
            """.trimIndent()
        val assets = mockk<AssetReader> { every { readText("vocabulary_pl.json") } returns json }

        val words = VocabularySeedAssetLoader(assets).load().map { it.toWord() }

        assertEquals(PartOfSpeech.PREPOSITION, words[0].partOfSpeech)
        assertNull(words[0].forms)
        assertNull(words[1].partOfSpeech)
    }
}
