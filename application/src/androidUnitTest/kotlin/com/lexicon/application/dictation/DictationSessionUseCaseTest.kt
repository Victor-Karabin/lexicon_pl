package com.lexicon.application.dictation

import com.lexicon.application.settings.StepCountResolver
import com.lexicon.application.training.FakeSessionStore
import com.lexicon.boundary.SessionStore
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.dictation.StartDictationSessionRequest
import com.lexicon.interactors.dictation.SubmitDictationAnswerRequest
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DictationSessionUseCaseTest {
    private val vocabulary: VocabularyRepository = mockk()
    private val settings: com.lexicon.boundary.SettingsRepository = mockk(relaxed = true)
    private val recordAnswer: RecordAnswerUseCase = mockk(relaxed = true)
    private val sessions: SessionStore = FakeSessionStore()

    private val start = StartDictationSessionUseCaseImpl(vocabulary, StepCountResolver(settings), sessions)
    private val submit = SubmitDictationAnswerUseCaseImpl(recordAnswer, AnswerNormalizer(), sessions)

    private suspend fun startSession(): String {
        coEvery { vocabulary.getRandomItems(any(), any()) } returns listOf(
            Word(VocabularyId(1), "kot", "cat", "kɔt"),
            Word(VocabularyId(2), "pies", "dog", "pjɛs"),
        )
        return start(StartDictationSessionRequest(stepCount = 2)).sessionId
    }

    private fun answer(
        sessionId: String,
        submitted: String,
        expectedText: String = "kot",
        stepIndex: Int = 0,
    ) = SubmitDictationAnswerRequest(
        sessionId = sessionId,
        stepIndex = stepIndex,
        vocabularyItemId = 1,
        expectedText = expectedText,
        submittedText = submitted,
        tipUsed = false,
        skipped = false,
    )

    @Test
    fun `starting a session records its steps against the session, not the caller`() =
        runTest {
            val id = startSession()
            val session = sessions.find(SessionId(id))!!

            assertEquals(2, session.steps.size)
            assertEquals("kot", session.step(0).expectedAnswer)
            assertEquals(VocabularyId(2), session.step(1).wordId)
        }

    @Test
    fun `the expected answer comes from the session even when the caller claims another`() =
        runTest {
            val id = startSession()

            val response = submit(answer(id, submitted = "pies", expectedText = "pies"))

            assertEquals("a caller cannot rename the right answer", StepOutcome.INCORRECT, response.outcome)
            assertEquals("kot", response.expectedText)
        }

    @Test
    fun `answering a step marks it on the session`() =
        runTest {
            val id = startSession()

            submit(answer(id, submitted = "kot"))

            val session = sessions.find(SessionId(id))!!
            assertEquals(StepOutcome.CORRECT, session.step(0).outcome)
            assertEquals(1, session.correctCount)
            assertEquals(1, session.currentStep?.index)
        }

    @Test
    fun `the session is complete once every step is answered`() =
        runTest {
            val id = startSession()

            submit(answer(id, submitted = "kot", stepIndex = 0))
            submit(answer(id, submitted = "pies", expectedText = "pies", stepIndex = 1))

            assertEquals(true, sessions.find(SessionId(id))!!.isComplete)
        }

    @Test
    fun `the recorded answer carries the session's word, not the caller's`() =
        runTest {
            val id = startSession()

            submit(answer(id, submitted = "pies", stepIndex = 1))

            coVerify { recordAnswer(match<RecordedAnswer> { it.vocabularyItemId == 2L }) }
        }
}
