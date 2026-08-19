package com.lexicon.domain.imagetest

import com.lexicon.domain.training.FakeSessionStore
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerRequest
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.model.training.StepOutcome
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitImageTestAnswerUseCaseImplTest {
    private val recordAnswer: RecordAnswerUseCase = mockk(relaxed = true)
    private val useCase = SubmitImageTestAnswerUseCaseImpl(recordAnswer, FakeSessionStore())

    private fun request(
        selectedOption: String?,
        skipped: Boolean = false,
    ) = SubmitImageTestAnswerRequest(
        sessionId = "session-1",
        stepIndex = 0,
        vocabularyItemId = 1L,
        correctOption = "cat",
        selectedOption = selectedOption,
        skipped = skipped,
    )

    @Test
    fun `selecting the correct option is Correct`() =
        runTest {
            val response = useCase(request(selectedOption = "cat"))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `selecting a distractor is Incorrect`() =
        runTest {
            val response = useCase(request(selectedOption = "dog"))
            assertEquals(StepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `skip is Skipped regardless of selection`() =
        runTest {
            val response = useCase(request(selectedOption = null, skipped = true))
            assertEquals(StepOutcome.SKIPPED, response.outcome)
        }
}
