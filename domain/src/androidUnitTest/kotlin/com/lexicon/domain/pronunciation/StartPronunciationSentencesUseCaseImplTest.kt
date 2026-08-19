package com.lexicon.domain.pronunciation

import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.SentenceRequestBoundary
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.pronunciation.PronunciationSentencesResult
import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
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

    private fun givenStudySet(vararg words: String) {
        coEvery { settings.getSettings() } returns
            AppSettingsBoundary(themeMode = ThemeModeBoundary.SYSTEM, stepCount = stepCount)
        val items = words.mapIndexed { index, word ->
            Word(VocabularyId(index + 1L), word, "meaning of $word", "x", cefr = CefrLevel.A1)
        }
        coEvery { vocabulary.studySetWordIds() } returns items.map { it.id.value }
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
            givenStudySet("okno", "dom", "kot", "lampa", "stół")
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
            givenStudySet("okno", "dom", "kot", "lampa", "stół")
            generating("To jest")

            val session = (useCase() as PronunciationSentencesResult.Ready).session

            assertTrue(session.steps.all { it.vocabularyItemId in 1L..5L })
            assertEquals(session.steps.size, session.steps.map { it.stepIndex }.distinct().size)
        }

    @Test
    fun `steps are numbered from zero in order`() =
        runTest {
            givenStudySet("okno", "dom", "kot", "lampa", "stół")
            generating("To jest")

            val session = (useCase() as PronunciationSentencesResult.Ready).session

            assertEquals(session.steps.indices.toList(), session.steps.map { it.stepIndex })
        }

    @Test
    fun `the number of sentences follows the step count setting`() =
        runTest {
            stepCount = 4
            givenStudySet("okno", "dom", "kot", "lampa", "stol", "zegar", "dywan")
            generating("To jest")

            val session = (useCase() as PronunciationSentencesResult.Ready).session

            assertEquals(4, session.steps.size)
        }

    @Test
    fun `there is never more to read than there are studySet`() =
        runTest {
            stepCount = 20
            givenStudySet("okno", "dom")
            generating("To jest")

            val session = (useCase() as PronunciationSentencesResult.Ready).session

            assertEquals(2, session.steps.size)
        }

    @Test
    fun `no studySet means nothing to read`() =
        runTest {
            coEvery { vocabulary.studySetWordIds() } returns emptyList()
            coEvery { vocabulary.getItemsByIds(any()) } returns emptyList()

            assertEquals(PronunciationSentencesResult.EmptyStudySet, useCase())
        }

    @Test
    fun `being offline is reported rather than showing an empty training`() =
        runTest {
            givenStudySet("okno", "dom")
            coEvery { generator.generate(any()) } returns SentenceResultBoundary.Offline

            assertEquals(PronunciationSentencesResult.Offline, useCase())
        }

    @Test
    fun `a refusal carries its reason`() =
        runTest {
            givenStudySet("okno", "dom")
            coEvery { generator.generate(any()) } returns SentenceResultBoundary.Refused("busy")

            assertEquals(PronunciationSentencesResult.Refused("busy"), useCase())
        }
}
