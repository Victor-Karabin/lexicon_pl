package com.lexicon.domain.dictationpuzzle

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.common.Clock
import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerRequest
import com.lexicon.interactors.training.StepOutcome
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitDictationPuzzleAnswerUseCaseImplTest {
    private val trainingHistoryRepository: TrainingHistoryRepository = mockk(relaxed = true)
    private val clock: Clock = mockk { every { nowEpochMillis() } returns 1_000L }
    private val useCase = SubmitDictationPuzzleAnswerUseCaseImpl(trainingHistoryRepository, AnswerNormalizer(), clock)

    private fun request(
        submittedText: String = "kot",
        tipUsed: Boolean = false,
        skipped: Boolean = false,
    ) = SubmitDictationPuzzleAnswerRequest(
        sessionId = "session-1",
        stepIndex = 0,
        vocabularyItemId = 1L,
        expectedText = "kot",
        submittedText = submittedText,
        tipUsed = tipUsed,
        skipped = skipped,
    )

    @Test
    fun `matching tiles without tip is Correct`() =
        runTest {
            val response = useCase(request(submittedText = "kot"))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `matching tiles with tip used is still Correct — tip usage doesn't affect the outcome`() =
        runTest {
            val response = useCase(request(submittedText = "kot", tipUsed = true))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `tip usage is recorded to history regardless of outcome`() =
        runTest {
            useCase(request(submittedText = "kot", tipUsed = true))

            coVerify {
                trainingHistoryRepository.recordResult(
                    match<TrainingResultBoundary> { it.tipUsed },
                )
            }
        }

    @Test
    fun `skip is Skipped regardless of tip`() =
        runTest {
            val response = useCase(request(skipped = true, tipUsed = true))
            assertEquals(StepOutcome.SKIPPED, response.outcome)
        }
}
