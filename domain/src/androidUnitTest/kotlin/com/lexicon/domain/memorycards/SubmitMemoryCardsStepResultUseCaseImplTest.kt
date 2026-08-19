package com.lexicon.domain.memorycards

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.interactors.memorycards.SubmitMemoryCardsStepResultRequest
import com.lexicon.interactors.training.StepOutcome
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitMemoryCardsStepResultUseCaseImplTest {
    private val trainingHistoryRepository: TrainingHistoryRepository = mockk(relaxed = true)
    private val clock: Clock = mockk { every { nowEpochMillis() } returns 1_000L }
    private val useCase = SubmitMemoryCardsStepResultUseCaseImpl(trainingHistoryRepository, clock)

    @Test
    fun `zero incorrect attempts is Correct`() =
        runTest {
            val response =
                useCase(
                    SubmitMemoryCardsStepResultRequest("s", 0, listOf(1L, 2L), incorrectAttempts = 0, skipped = false),
                )
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `any incorrect attempt makes the completed step Incorrect`() =
        runTest {
            val response =
                useCase(
                    SubmitMemoryCardsStepResultRequest("s", 0, listOf(1L, 2L), incorrectAttempts = 2, skipped = false),
                )
            assertEquals(StepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `skip is Skipped regardless of attempts`() =
        runTest {
            val response =
                useCase(
                    SubmitMemoryCardsStepResultRequest("s", 0, listOf(1L, 2L), incorrectAttempts = 0, skipped = true),
                )
            assertEquals(StepOutcome.SKIPPED, response.outcome)
        }

    @Test
    fun `records one result row per pair, sharing the step outcome`() =
        runTest {
            useCase(SubmitMemoryCardsStepResultRequest("s", 0, listOf(1L, 2L, 3L), incorrectAttempts = 0, skipped = false))

            coVerify(exactly = 3) {
                trainingHistoryRepository.recordResult(
                    match<TrainingResultBoundary> { it.outcome == TrainingResultOutcomeBoundary.CORRECT },
                )
            }
        }
}
