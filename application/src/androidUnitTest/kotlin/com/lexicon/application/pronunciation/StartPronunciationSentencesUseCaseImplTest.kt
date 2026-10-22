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

    private fun givenWords(vararg words: Word) {
        givenStepCount()
        coEvery { vocabulary.studySetWordIds() } returns words.map { it.id.value }
        coEvery { vocabulary.getItemsByIds(any()) } returns words.toList()
    }

    private fun capturingRequests(): List<SentenceRequestBoundary> {
        val requests = mutableListOf<SentenceRequestBoundary>()
        coEvery { generator.generate(capture(requests)) } answers {
            SentenceResultBoundary.Generated("To jest ${firstArg<SentenceRequestBoundary>().word}.")
        }
        return requests
    }

    @Test
    fun `offline wins over a refusal`() =
        runTest {
            givenStudySet("okno", "dom")
            coEvery { generator.generate(any()) } answers {
                if (firstArg<SentenceRequestBoundary>().word == "okno") {
                    SentenceResultBoundary.Offline
                } else {
                    SentenceResultBoundary.Refused("busy")
                }
            }

            assertEquals(PronunciationSentencesResult.Offline, start())
        }

    @Test
    fun `a refusal reason is passed on when the other sentences came back blank`() =
        runTest {
            givenStudySet("okno", "dom")
            coEvery { generator.generate(any()) } answers {
                if (firstArg<SentenceRequestBoundary>().word == "okno") {
                    SentenceResultBoundary.Refused("busy")
                } else {
                    SentenceResultBoundary.Generated("   ")
                }
            }

            assertEquals(PronunciationSentencesResult.Refused("busy"), start())
        }

    @Test
    fun `a blank sentence is dropped and the rest keep their order`() =
        runTest {
            givenStudySet("okno", "dom", "kot")
            coEvery { generator.generate(any()) } answers {
                val word = firstArg<SentenceRequestBoundary>().word
                if (word == "dom") SentenceResultBoundary.Generated(" \t ") else SentenceResultBoundary.Generated("To jest $word.")
            }

            val session = (start() as PronunciationSentencesResult.Ready).session

            assertEquals(listOf(0, 1), session.steps.map { it.stepIndex })
            assertTrue(session.steps.none { it.expectedText.contains("dom") })
            assertEquals(2, session.steps.size)
        }

    @Test
    fun `a sentence is trimmed before it is used`() =
        runTest {
            givenStudySet("okno")
            coEvery { generator.generate(any()) } returns SentenceResultBoundary.Generated("  To jest okno.  ")

            val session = (start() as PronunciationSentencesResult.Ready).session

            assertEquals(listOf("To jest okno."), session.steps.map { it.expectedText })
            assertEquals(listOf("To jest okno."), session.steps.map { it.clueText })
        }

    @Test
    fun `when no sentence is usable the result is refused with a fixed reason`() =
        runTest {
            givenStudySet("okno", "dom")
            coEvery { generator.generate(any()) } returns SentenceResultBoundary.Generated("   ")

            assertEquals(PronunciationSentencesResult.Refused("no sentence came back"), start())
        }

    @Test
    fun `sentences are asked for with no context and no required words`() =
        runTest {
            givenWords(Word(VocabularyId(1), "okno", "window", "x", cefr = CefrLevel.B2))
            val requests = capturingRequests()

            start()

            assertEquals(
                listOf(
                    SentenceRequestBoundary(
                        word = "okno",
                        translation = "window",
                        level = "B2",
                        context = "",
                        requiredWords = emptyList(),
                    ),
                ),
                requests,
            )
        }

    @Test
    fun `the level is the highest level among the words asked about and A1 without any`() =
        runTest {
            givenWords(
                Word(VocabularyId(1), "okno", "x", "x", cefr = CefrLevel.A2),
                Word(VocabularyId(2), "dom", "x", "x", cefr = CefrLevel.C1),
            )
            val requests = capturingRequests()

            start()

            assertTrue(requests.all { it.level == "C1" })

            givenWords(Word(VocabularyId(1), "okno", "x", "x"), Word(VocabularyId(2), "dom", "x", "x"))
            val plainRequests = capturingRequests()

            start()

            assertTrue(plainRequests.all { it.level == "A1" })
        }

    @Test
    fun `no spare sentences are asked for beyond the step count`() =
        runTest {
            stepCount = 3
            givenStudySet("okno", "dom", "kot", "lampa", "stol", "zegar", "dywan")
            val requests = capturingRequests()

            start()

            assertEquals(3, requests.size)
            assertEquals(3, requests.map { it.word }.toSet().size)
        }
}
