package com.lexicon.domain.pronunciation

import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.SentenceRequestBoundary
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.pronunciation.PronunciationSentencesResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartPronunciationSentencesUseCaseImplTest {
    private val vocabulary: VocabularyRepository = mockk()
    private val generator: SentenceGenerator = mockk()
    private val settings: SettingsRepository = mockk()
    private val useCase = StartPronunciationSentencesUseCaseImpl(vocabulary, generator, StepCountResolver(settings))

    private var stepCount = 5

    private fun givenFavourites(vararg words: String) {
        coEvery { settings.getSettings() } returns
            AppSettingsBoundary(themeMode = ThemeModeBoundary.SYSTEM, stepCount = stepCount)
        val items = words.mapIndexed { index, word ->
            VocabularyItemBoundary(index + 1L, word, "meaning of $word", "x", cefr = "A1")
        }
        coEvery { vocabulary.favouriteWordIds() } returns items.map { it.id }
        coEvery { vocabulary.getItemsByIds(any()) } returns items
    }

    private fun generating(sentence: String) {
        coEvery { generator.generate(any()) } answers {
            SentenceResultBoundary.Generated("$sentence ${firstArg<SentenceRequestBoundary>().word}.")
        }
    }

    @Test
    fun `the sentence is both what is shown and what has to be said`() =
        runTest {
            givenFavourites("okno", "dom", "kot", "lampa", "stół")
            generating("To jest")

            val session = (useCase() as PronunciationSentencesResult.Ready).session

            assertTrue(session.steps.isNotEmpty())
            session.steps.forEach { step ->
                assertEquals(step.expectedText, step.clueText)
                assertTrue(step.expectedText.startsWith("To jest"))
            }
        }

    @Test
    fun `each sentence is recorded against the word it was written for`() =
        runTest {
            givenFavourites("okno", "dom", "kot", "lampa", "stół")
            generating("To jest")

            val session = (useCase() as PronunciationSentencesResult.Ready).session

            assertTrue(session.steps.all { it.vocabularyItemId in 1L..5L })
            assertEquals(session.steps.size, session.steps.map { it.stepIndex }.distinct().size)
        }

    @Test
    fun `steps are numbered from zero in order`() =
        runTest {
            givenFavourites("okno", "dom", "kot", "lampa", "stół")
            generating("To jest")

            val session = (useCase() as PronunciationSentencesResult.Ready).session

            assertEquals(session.steps.indices.toList(), session.steps.map { it.stepIndex })
        }

    @Test
    fun `the number of sentences follows the step count setting`() =
        runTest {
            stepCount = 4
            givenFavourites("okno", "dom", "kot", "lampa", "stol", "zegar", "dywan")
            generating("To jest")

            val session = (useCase() as PronunciationSentencesResult.Ready).session

            assertEquals(4, session.steps.size)
        }

    @Test
    fun `there is never more to read than there are favourites`() =
        runTest {
            stepCount = 20
            givenFavourites("okno", "dom")
            generating("To jest")

            val session = (useCase() as PronunciationSentencesResult.Ready).session

            assertEquals(2, session.steps.size)
        }

    @Test
    fun `no favourites means nothing to read`() =
        runTest {
            coEvery { vocabulary.favouriteWordIds() } returns emptyList()
            coEvery { vocabulary.getItemsByIds(any()) } returns emptyList()

            assertEquals(PronunciationSentencesResult.NoFavourites, useCase())
        }

    @Test
    fun `being offline is reported rather than showing an empty training`() =
        runTest {
            givenFavourites("okno", "dom")
            coEvery { generator.generate(any()) } returns SentenceResultBoundary.Offline

            assertEquals(PronunciationSentencesResult.Offline, useCase())
        }

    @Test
    fun `a refusal carries its reason`() =
        runTest {
            givenFavourites("okno", "dom")
            coEvery { generator.generate(any()) } returns SentenceResultBoundary.Refused("busy")

            assertEquals(PronunciationSentencesResult.Refused("busy"), useCase())
        }
}
