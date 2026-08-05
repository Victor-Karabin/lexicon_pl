package com.lexicon.domain.dictation

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.interactors.dictation.DictationStepOutcome
import com.lexicon.interactors.dictation.SubmitDictationAnswerRequest
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitDictationAnswerUseCaseImplTest {
    private val trainingHistoryRepository: TrainingHistoryRepository = mockk(relaxed = true)
    private val clock: Clock = mockk { every { nowEpochMillis() } returns 1_000L }
    private val useCase =
        SubmitDictationAnswerUseCaseImpl(
            trainingHistoryRepository = trainingHistoryRepository,
            answerNormalizer = AnswerNormalizer(),
            clock = clock,
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
            assertEquals(DictationStepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `non-matching answer is Incorrect`() =
        runTest {
            val response = useCase(request(submittedText = "pies"))
            assertEquals(DictationStepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `matching answer with tip used is still Incorrect`() =
        runTest {
            val response = useCase(request(submittedText = "kot", tipUsed = true))
            assertEquals(DictationStepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `skipped step is Skipped even if text happens to be submitted`() =
        runTest {
            val response = useCase(request(submittedText = "kot", skipped = true))
            assertEquals(DictationStepOutcome.SKIPPED, response.outcome)
        }

    @Test
    fun `records the result to history with the resolved outcome`() =
        runTest {
            useCase(request(submittedText = "kot"))

            coVerify {
                trainingHistoryRepository.recordResult(
                    match<TrainingResultBoundary> { it.outcome == TrainingResultOutcomeBoundary.CORRECT },
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
