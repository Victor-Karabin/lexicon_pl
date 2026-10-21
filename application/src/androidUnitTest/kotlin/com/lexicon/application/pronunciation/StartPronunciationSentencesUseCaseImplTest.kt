package com.lexicon.application.pronunciation

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.application.training.FakeSessionStore
import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.SentenceRequestBoundary
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.pronunciation.PronunciationSentencesResult
import com.lexicon.interactors.pronunciation.StartPronunciationSessionRequest
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.Step
import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartPronunciationSentencesUseCaseImplTest {
    private val vocabulary: VocabularyRepository = mockk()
    private val generator: SentenceGenerator = mockk()
    private val settings: SettingsRepository = mockk()
    private val sessions = FakeSessionStore()
    private val useCase = StartPronunciationSentencesUseCaseImpl(vocabulary, generator, StepCountResolver(settings), sessions)

    private var stepCount = 5

    private val requested = mutableListOf<String>()

    private fun items(vararg words: String) =
        words.mapIndexed { index, word ->
            Word(VocabularyId(index + 1L), word, "meaning of $word", "x", cefr = CefrLevel.A1)
        }

    private fun givenStepCount() {
        coEvery { settings.getSettings() } returns
            AppSettingsBoundary(themeMode = ThemeModeBoundary.SYSTEM, stepCount = stepCount)
    }

    private fun givenStudySet(vararg words: String) {
        givenStepCount()
        val items = items(*words)
        coEvery { vocabulary.studySetWordIds() } returns items.map { it.id.value }
        coEvery { vocabulary.getItemsByIds(any()) } returns items
    }

    private fun generating(sentence: String) {
        coEvery { generator.generate(any()) } answers {
            val word = firstArg<SentenceRequestBoundary>().word
            requested += word
            SentenceResultBoundary.Generated("$sentence $word.")
        }
    }

    private suspend fun start(request: StartPronunciationSessionRequest = StartPronunciationSessionRequest()) = useCase(request)

    @Test
    fun `the sentence is both what is shown and what has to be said`() =
        runTest {
            givenStudySet("okno", "dom", "kot", "lampa", "stół")
            generating("To jest")

            val session = (start() as PronunciationSentencesResult.Ready).session

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

            val session = (start() as PronunciationSentencesResult.Ready).session

            assertTrue(session.steps.all { it.vocabularyItemId in 1L..5L })
            assertEquals(session.steps.size, session.steps.map { it.stepIndex }.distinct().size)
        }

    @Test
    fun `steps are numbered from zero in order`() =
        runTest {
            givenStudySet("okno", "dom", "kot", "lampa", "stół")
            generating("To jest")

            val session = (start() as PronunciationSentencesResult.Ready).session

            assertEquals(session.steps.indices.toList(), session.steps.map { it.stepIndex })
        }

    @Test
    fun `the number of sentences follows the step count setting`() =
        runTest {
            stepCount = 4
            givenStudySet("okno", "dom", "kot", "lampa", "stol", "zegar", "dywan")
            generating("To jest")

            val session = (start() as PronunciationSentencesResult.Ready).session

            assertEquals(4, session.steps.size)
        }

    @Test
    fun `there is never more to read than there are studySet`() =
        runTest {
            stepCount = 20
            givenStudySet("okno", "dom")
            generating("To jest")

            val session = (start() as PronunciationSentencesResult.Ready).session

            assertEquals(2, session.steps.size)
        }

    @Test
    fun `no studySet means nothing to read`() =
        runTest {
            coEvery { vocabulary.studySetWordIds() } returns emptyList()
            coEvery { vocabulary.getItemsByIds(any()) } returns emptyList()

            assertEquals(PronunciationSentencesResult.EmptyStudySet, start())
        }

    @Test
    fun `being offline is reported rather than showing an empty training`() =
        runTest {
            givenStudySet("okno", "dom")
            coEvery { generator.generate(any()) } returns SentenceResultBoundary.Offline

            assertEquals(PronunciationSentencesResult.Offline, start())
        }

    @Test
    fun `a refusal carries its reason`() =
        runTest {
            givenStudySet("okno", "dom")
            coEvery { generator.generate(any()) } returns SentenceResultBoundary.Refused("busy")

            assertEquals(PronunciationSentencesResult.Refused("busy"), start())
        }

    @Test
    fun `one sentence that does not come back leaves the rest to read`() =
        runTest {
            givenStudySet("okno", "dom", "kot")
            coEvery { generator.generate(any()) } answers {
                val word = firstArg<SentenceRequestBoundary>().word
                if (word == "dom") SentenceResultBoundary.Offline else SentenceResultBoundary.Generated("To jest $word.")
            }

            val session = (start() as PronunciationSentencesResult.Ready).session

            assertEquals(listOf(0, 1), session.steps.map { it.stepIndex })
            assertTrue(session.steps.none { it.expectedText.contains("dom") })
        }

    @Test
    fun `a lesson's word list is the only thing sentences are written for and the study set is not read`() =
        runTest {
            givenStepCount()
            val lesson = listOf(2L, 3L)
            coEvery { vocabulary.getItemsByIds(lesson) } returns items("okno", "dom", "kot").filter { it.id.value in lesson }
            generating("To jest")

            val session = (start(StartPronunciationSessionRequest(vocabularyIds = lesson)) as PronunciationSentencesResult.Ready).session

            assertEquals(setOf("dom", "kot"), requested.toSet())
            assertEquals(setOf(2L, 3L), session.steps.map { it.vocabularyItemId }.toSet())
            coVerify(exactly = 0) { vocabulary.studySetWordIds() }
        }

    @Test
    fun `without a word list the study set is used`() =
        runTest {
            givenStudySet("okno", "dom")
            generating("To jest")

            start(StartPronunciationSessionRequest(vocabularyIds = emptyList()))

            assertEquals(setOf("okno", "dom"), requested.toSet())
            coVerify(exactly = 1) { vocabulary.studySetWordIds() }
        }

    @Test
    fun `a requested step count is honoured over the setting`() =
        runTest {
            stepCount = 5
            givenStudySet("okno", "dom", "kot", "lampa", "stol")
            generating("To jest")

            val session = (start(StartPronunciationSessionRequest(stepCount = 2)) as PronunciationSentencesResult.Ready).session

            assertEquals(2, session.steps.size)
        }

    @Test
    fun `a lesson with fewer words than the step count gives fewer sentences and no top-up`() =
        runTest {
            stepCount = 5
            givenStepCount()
            val lesson = listOf(1L, 2L)
            coEvery { vocabulary.getItemsByIds(lesson) } returns items("okno", "dom")
            generating("To jest")

            val session = (start(StartPronunciationSessionRequest(vocabularyIds = lesson)) as PronunciationSentencesResult.Ready).session

            assertEquals(2, session.steps.size)
            assertEquals(setOf("okno", "dom"), requested.toSet())
        }

    @Test
    fun `a lesson word whose sentence does not come back leaves one sentence fewer`() =
        runTest {
            givenStepCount()
            val lesson = listOf(1L, 2L, 3L)
            coEvery { vocabulary.getItemsByIds(lesson) } returns items("okno", "dom", "kot")
            coEvery { generator.generate(any()) } answers {
                val word = firstArg<SentenceRequestBoundary>().word
                if (word == "dom") SentenceResultBoundary.Offline else SentenceResultBoundary.Generated("To jest $word.")
            }

            val session = (start(StartPronunciationSessionRequest(vocabularyIds = lesson)) as PronunciationSentencesResult.Ready).session

            assertEquals(2, session.steps.size)
        }

    @Test
    fun `a stored session holds the generated sentences for the lesson's words`() =
        runTest {
            givenStepCount()
            val lesson = listOf(1L, 2L)
            coEvery { vocabulary.getItemsByIds(lesson) } returns items("okno", "dom")
            generating("To jest")

            val session = (start(StartPronunciationSessionRequest(vocabularyIds = lesson)) as PronunciationSentencesResult.Ready).session

            val stored = checkNotNull(sessions.find(SessionId(session.sessionId)))
            assertEquals(session.steps.map { it.expectedText }, stored.steps.map { (it as Step.Question).expectedAnswer })
        }
}
