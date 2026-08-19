package com.lexicon.domain.wordmatch

import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.interactors.wordmatch.SubmitWordMatchStepResultRequest
import com.lexicon.model.training.StepOutcome
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitWordMatchStepResultUseCaseImplTest {
    private val recordAnswer: RecordAnswerUseCase = mockk(relaxed = true)
    private val useCase = SubmitWordMatchStepResultUseCaseImpl(recordAnswer)

    @Test
    fun `zero incorrect attempts is Correct`() =
        runTest {
            val response =
                useCase(SubmitWordMatchStepResultRequest("s", 0, listOf(1L, 2L), incorrectAttempts = 0))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `any incorrect attempt makes the completed step Incorrect`() =
        runTest {
            val response =
                useCase(SubmitWordMatchStepResultRequest("s", 0, listOf(1L, 2L), incorrectAttempts = 3))
            assertEquals(StepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `records one result row per pair, sharing the step outcome`() =
        runTest {
            useCase(SubmitWordMatchStepResultRequest("s", 0, listOf(1L, 2L, 3L), incorrectAttempts = 0))

            coVerify(exactly = 3) {
                recordAnswer(
                    match<RecordedAnswer> { it.outcome == StepOutcome.CORRECT },
                )
            }
        }
}
