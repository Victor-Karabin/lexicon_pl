package com.lexicon.application.pronunciation

import com.lexicon.application.dictation.AnswerNormalizer
import com.lexicon.application.settings.StepCountResolver
import com.lexicon.application.training.FakeSessionStore
import com.lexicon.application.training.open
import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.SentenceRequestBoundary
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.pronunciation.PronunciationSentencesResult
import com.lexicon.interactors.pronunciation.StartPronunciationSessionRequest
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultRequest
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.Step
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PronunciationSentencesRecordingTest {
    private val recordAnswer: RecordAnswerUseCase = mockk(relaxed = true)
    private val sessions = FakeSessionStore()
    private val submit = SubmitPronunciationResultUseCaseImpl(recordAnswer, AnswerNormalizer(), sessions)

    private val vocabulary: VocabularyRepository = mockk()
    private val generator: SentenceGenerator = mockk()
    private val settings: SettingsRepository = mockk()
    private val start = StartPronunciationSentencesUseCaseImpl(
        vocabulary = vocabulary,
        generator = generator,
        stepCountResolver = StepCountResolver(settings),
        sessions = sessions,
    )

    private fun request(
        sessionId: String,
        expected: String,
        recognized: String,
    ) = SubmitPronunciationResultRequest(
        sessionId = sessionId,
        stepIndex = 0,
        vocabularyItemId = 1L,
        expectedText = expected,
        recognizedText = recognized,
        confidence = null,
        tipUsed = false,
        skipped = false,
    )

    @Test
    fun `saying a sentence is recorded as Pronunciation of sentences`() =
        runTest {
            val id = sessions.open(TrainingType.PRONUNCIATION_SENTENCES, listOf(VocabularyId(1) to "To jest kot."))

            submit(request(id.value, expected = "To jest kot.", recognized = "to jest kot"))

            coVerify {
                recordAnswer(
                    match<RecordedAnswer> {
                        it.trainingType == TrainingType.PRONUNCIATION_SENTENCES &&
                            it.vocabularyItemId == 1L &&
                            it.expectedAnswer == "To jest kot."
                    },
                )
            }
        }

    @Test
    fun `saying a single word is still recorded as Pronunciation check`() =
        runTest {
            val id = sessions.open(TrainingType.PRONUNCIATION_CHECK, listOf(VocabularyId(1) to "kot"))

            submit(request(id.value, expected = "kot", recognized = "kot"))

            coVerify { recordAnswer(match<RecordedAnswer> { it.trainingType == TrainingType.PRONUNCIATION_CHECK }) }
        }

    @Test
    fun `a session that was never stored records nothing, though the learner still gets the verdict`() =
        runTest {
            val response = submit(request("unknown", expected = "kot", recognized = "kot"))

            coVerify(exactly = 0) { recordAnswer(any()) }
            assertEquals(StepOutcome.CORRECT, response.outcome)
            assertEquals("kot", response.expectedText)
        }

    @Test
    fun `a sentence session is stored with the sentences to be said`() =
        runTest {
            coEvery { settings.getSettings() } returns AppSettingsBoundary(ThemeModeBoundary.SYSTEM, stepCount = 5)
            val words = listOf("okno", "dom").mapIndexed { index, text ->
                Word(VocabularyId(index + 1L), text, "meaning", "x", cefr = CefrLevel.A1)
            }
            coEvery { vocabulary.studySetWordIds() } returns listOf(1L, 2L)
            coEvery { vocabulary.getItemsByIds(any()) } returns words
            coEvery { generator.generate(any()) } answers {
                SentenceResultBoundary.Generated("To jest ${firstArg<SentenceRequestBoundary>().word}.")
            }

            val response = (start(StartPronunciationSessionRequest()) as PronunciationSentencesResult.Ready).session

            val stored = checkNotNull(sessions.find(SessionId(response.sessionId)))
            assertEquals(TrainingType.PRONUNCIATION_SENTENCES, stored.training)
            response.steps.forEach { step ->
                val question = stored.step(step.stepIndex) as Step.Question
                assertEquals(step.vocabularyItemId, question.wordId.value)
                assertEquals(step.expectedText, question.expectedAnswer)
            }
        }
}
