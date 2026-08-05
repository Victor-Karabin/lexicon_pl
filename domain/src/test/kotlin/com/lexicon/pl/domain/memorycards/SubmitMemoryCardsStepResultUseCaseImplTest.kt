package com.lexicon.pl.domain.memorycards

import com.lexicon.pl.boundary.TrainingHistoryRepository
import com.lexicon.pl.boundary.TrainingResultBoundary
import com.lexicon.pl.boundary.TrainingResultOutcomeBoundary
import com.lexicon.pl.common.Clock
import com.lexicon.pl.interactors.memorycards.MemoryCardsStepOutcome
import com.lexicon.pl.interactors.memorycards.SubmitMemoryCardsStepResultRequest
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
            assertEquals(MemoryCardsStepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `any incorrect attempt makes the completed step Incorrect`() =
        runTest {
            val response =
                useCase(
                    SubmitMemoryCardsStepResultRequest("s", 0, listOf(1L, 2L), incorrectAttempts = 2, skipped = false),
                )
            assertEquals(MemoryCardsStepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `skip is Skipped regardless of attempts`() =
        runTest {
            val response =
                useCase(
                    SubmitMemoryCardsStepResultRequest("s", 0, listOf(1L, 2L), incorrectAttempts = 0, skipped = true),
                )
            assertEquals(MemoryCardsStepOutcome.SKIPPED, response.outcome)
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
