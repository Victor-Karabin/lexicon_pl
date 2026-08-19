package com.lexicon.domain.pronunciation

import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.domain.training.FakeSessionStore
import com.lexicon.interactors.pronunciation.SubmitPronunciationResultRequest
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitPronunciationResultUseCaseImplTest {
    private val recordAnswer: RecordAnswerUseCase = mockk(relaxed = true)
    private val useCase = SubmitPronunciationResultUseCaseImpl(recordAnswer, AnswerNormalizer(), FakeSessionStore())

    private fun request(
        recognizedText: String = "kot",
        confidence: Float? = null,
        tipUsed: Boolean = false,
        skipped: Boolean = false,
    ) = SubmitPronunciationResultRequest(
        sessionId = "session-1",
        stepIndex = 0,
        vocabularyItemId = 1L,
        expectedText = "kot",
        recognizedText = recognizedText,
        confidence = confidence,
        tipUsed = tipUsed,
        skipped = skipped,
    )

    @Test
    fun `confidence at or above the 70pct threshold is Correct`() =
        runTest {
            val response = useCase(request(confidence = 0.7f))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `the right word said with low confidence is Correct`() =
        runTest {
            val response = useCase(request(recognizedText = "kot", confidence = 0.1f))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `the wrong word said with high confidence is Incorrect`() =
        runTest {
            val response = useCase(request(recognizedText = "pies", confidence = 0.99f))
            assertEquals(StepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `no confidence at all still compares the words`() =
        runTest {
            val response = useCase(request(recognizedText = "Kot", confidence = null))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `high confidence with tip used is still Correct — tip usage doesn't affect the outcome`() =
        runTest {
            val response = useCase(request(confidence = 0.99f, tipUsed = true))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `tip usage is recorded to history regardless of outcome`() =
        runTest {
            useCase(request(confidence = 0.99f, tipUsed = true))
            coVerify { recordAnswer(match<RecordedAnswer> { it.tipUsed }) }
        }

    @Test
    fun `skip is Skipped regardless of confidence`() =
        runTest {
            val response = useCase(request(confidence = 0.99f, skipped = true))
            assertEquals(StepOutcome.SKIPPED, response.outcome)
        }
}
