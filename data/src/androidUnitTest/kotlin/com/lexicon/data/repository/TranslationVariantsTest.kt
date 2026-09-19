package com.lexicon.data.repository

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.TranslationSuggester
import com.lexicon.data.local.VocabularySeeder
import com.lexicon.data.local.WordDao
import com.lexicon.data.local.WordEntity
import com.lexicon.model.vocabulary.CefrLevel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranslationVariantsTest {
    private val corpus: TranslationSuggester = mockk()
    private val remote: TranslationSuggester = mockk()

    private val wordDao: WordDao = mockk()
    private val seeder: VocabularySeeder = mockk(relaxed = true)

    private fun entity(
        text: String,
        translation: String,
        cefr: String = "",
    ) = WordEntity(id = 1, text = text, translation = translation, transcription = "", cefr = cefr)

    @Test
    fun `the corpus is asked first and the remote service only for what is missing`() =
        runTest {
            coEvery { corpus.suggest("water", any(), 4) } returns listOf("woda")
            coEvery { remote.suggest("water", any(), 3) } returns listOf("wódka", "woda", "wodzianka")

            val merged = MergingTranslationSuggester(listOf(corpus, remote))

            assertEquals(
                listOf("woda", "wódka", "wodzianka"),
                merged.suggest("water", TranslationDirection.EN_TO_PL, limit = 4),
            )
        }

    @Test
    fun `a source that fails does not sink the variants the others found`() =
        runTest {
            coEvery { corpus.suggest(any(), any(), any()) } throws IllegalStateException("no database")
            coEvery { remote.suggest("water", any(), 4) } returns listOf("woda")

            val merged = MergingTranslationSuggester(listOf(corpus, remote))

            assertEquals(listOf("woda"), merged.suggest("water", TranslationDirection.EN_TO_PL, limit = 4))
        }

    @Test
    fun `nothing is asked of anyone for a blank word`() =
        runTest {
            val merged = MergingTranslationSuggester(listOf(corpus, remote))

            assertEquals(emptyList<String>(), merged.suggest("   ", TranslationDirection.EN_TO_PL, limit = 4))
            coVerify(exactly = 0) { corpus.suggest(any(), any(), any()) }
        }

    @Test
    fun `the corpus offers only the words whose other side matches exactly`() =
        runTest {
            coEvery { wordDao.search(any(), any(), any(), any(), any(), any()) } returns listOf(
                entity("woda", "water"),
                entity("wodospad", "waterfall"),
                entity("wódka", "Water"),
            )

            val suggester = CorpusTranslationSuggester(wordDao, seeder)

            assertEquals(
                listOf("woda", "wódka"),
                suggester.suggest("water", TranslationDirection.EN_TO_PL, limit = 4),
            )
        }

    @Test
    fun `the level of a new word is the easiest the corpus knows for that meaning`() =
        runTest {
            coEvery { wordDao.search(any(), any(), any(), any(), any(), any()) } returns listOf(
                entity("hydrant", "water", cefr = "B2"),
                entity("woda", "water", cefr = "A1"),
                entity("wodospad", "waterfall", cefr = "A2"),
            )

            val guesser = CorpusWordLevelGuesser(wordDao, seeder)

            assertEquals(CefrLevel.A1, guesser.guess("wódka", "water"))
        }

    @Test
    fun `a meaning the corpus does not carry leaves the level unknown`() =
        runTest {
            coEvery { wordDao.search(any(), any(), any(), any(), any(), any()) } returns emptyList()

            val guesser = CorpusWordLevelGuesser(wordDao, seeder)

            assertNull(guesser.guess("wódka", "moonshine"))
        }

    @Test
    fun `an entry carrying several senses matches on any of them, verbs without their to`() =
        runTest {
            coEvery { wordDao.search(any(), any(), any(), any(), any(), any()) } returns listOf(
                entity("blisko", "near, by"),
                entity("jechać", "to travel, to drive"),
                entity("obok", "beside"),
            )

            val suggester = CorpusTranslationSuggester(wordDao, seeder)

            assertEquals(listOf("blisko"), suggester.suggest("by", TranslationDirection.EN_TO_PL, limit = 4))
            assertEquals(listOf("jechać"), suggester.suggest("drive", TranslationDirection.EN_TO_PL, limit = 4))
        }

    @Test
    fun `a Polish word with several senses offers each sense as its own variant`() =
        runTest {
            coEvery { wordDao.search(any(), any(), any(), any(), any(), any()) } returns listOf(
                entity("droga", "road, way"),
            )

            val suggester = CorpusTranslationSuggester(wordDao, seeder)

            assertEquals(listOf("road", "way"), suggester.suggest("droga", TranslationDirection.PL_TO_EN, limit = 4))
        }
}
