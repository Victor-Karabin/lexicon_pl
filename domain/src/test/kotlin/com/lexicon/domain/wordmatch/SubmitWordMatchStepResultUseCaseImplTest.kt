package com.lexicon.domain.wordmatch

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.boundary.TrainingResultOutcomeBoundary
import com.lexicon.common.Clock
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultRequest
import com.lexicon.interactors.wordmatch.WordMatchStepOutcome
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitWordMatchStepResultUseCaseImplTest {
    private val trainingHistoryRepository: TrainingHistoryRepository = mockk(relaxed = true)
    private val clock: Clock = mockk { every { nowEpochMillis() } returns 1_000L }
    private val useCase = SubmitWordMatchStepResultUseCaseImpl(trainingHistoryRepository, clock)

    @Test
    fun `zero incorrect attempts is Correct`() =
        runTest {
            val response =
                useCase(
                    SubmitWordMatchStepResultRequest("s", 0, listOf(1L, 2L), incorrectAttempts = 0, skipped = false),
                )
            assertEquals(WordMatchStepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `any incorrect attempt makes the completed step Incorrect`() =
        runTest {
            val response =
                useCase(
                    SubmitWordMatchStepResultRequest("s", 0, listOf(1L, 2L), incorrectAttempts = 3, skipped = false),
                )
            assertEquals(WordMatchStepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `skip is Skipped regardless of attempts`() =
        runTest {
            val response =
                useCase(
                    SubmitWordMatchStepResultRequest("s", 0, listOf(1L, 2L), incorrectAttempts = 0, skipped = true),
                )
            assertEquals(WordMatchStepOutcome.SKIPPED, response.outcome)
        }

    @Test
    fun `records one result row per pair, sharing the step outcome`() =
        runTest {
            useCase(SubmitWordMatchStepResultRequest("s", 0, listOf(1L, 2L, 3L), incorrectAttempts = 0, skipped = false))

            coVerify(exactly = 3) {
                trainingHistoryRepository.recordResult(
                    match<TrainingResultBoundary> { it.outcome == TrainingResultOutcomeBoundary.CORRECT },
                )
            }
        }
}
