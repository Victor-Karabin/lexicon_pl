package com.lexicon.domain.dictation

import com.lexicon.domain.training.FakeSessionStore
import com.lexicon.interactors.dictation.SubmitDictationAnswerRequest
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitDictationAnswerUseCaseImplTest {
    private val recordAnswer: RecordAnswerUseCase = mockk(relaxed = true)
    private val useCase =
        SubmitDictationAnswerUseCaseImpl(
            recordAnswer = recordAnswer,
            answerNormalizer = AnswerNormalizer(),
            sessions = FakeSessionStore(),
        )

    private fun request(
        submittedText: String = "kot",
        tipUsed: Boolean = false,
        skipped: Boolean = false,
    ) = SubmitDictationAnswerRequest(
        sessionId = "session-1",
        stepIndex = 0,
        vocabularyItemId = 1L,
        expectedText = "kot",
        submittedText = submittedText,
        tipUsed = tipUsed,
        skipped = skipped,
    )

    @Test
    fun `matching answer without tip is Correct`() =
        runTest {
            val response = useCase(request(submittedText = "Kot"))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `non-matching answer is Incorrect`() =
        runTest {
            val response = useCase(request(submittedText = "pies"))
            assertEquals(StepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `matching answer with tip used is still Correct — tip usage doesn't affect the outcome`() =
        runTest {
            val response = useCase(request(submittedText = "kot", tipUsed = true))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `tip usage is recorded to history regardless of outcome`() =
        runTest {
            useCase(request(submittedText = "kot", tipUsed = true))

            coVerify {
                recordAnswer(
                    match<RecordedAnswer> { it.tipUsed },
                )
            }
        }

    @Test
    fun `skipped step is Skipped even if text happens to be submitted`() =
        runTest {
            val response = useCase(request(submittedText = "kot", skipped = true))
            assertEquals(StepOutcome.SKIPPED, response.outcome)
        }

    @Test
    fun `records the result to history with the resolved outcome`() =
        runTest {
            useCase(request(submittedText = "kot"))

            coVerify {
                recordAnswer(
                    match<RecordedAnswer> { it.outcome == StepOutcome.CORRECT },
                )
            }
        }

    @Test
    fun `always returns the expected text for feedback display`() =
        runTest {
            val response = useCase(request(submittedText = "wrong"))
            assertEquals("kot", response.expectedText)
        }
}
