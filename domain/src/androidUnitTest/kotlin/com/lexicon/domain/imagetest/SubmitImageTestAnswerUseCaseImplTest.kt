package com.lexicon.domain.imagetest

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.imagetest.ImageTestStepOutcome
import com.lexicon.interactors.imagetest.SubmitImageTestAnswerRequest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitImageTestAnswerUseCaseImplTest {
    private val trainingHistoryRepository: TrainingHistoryRepository = mockk(relaxed = true)
    private val clock: Clock = mockk { every { nowEpochMillis() } returns 1_000L }
    private val useCase = SubmitImageTestAnswerUseCaseImpl(trainingHistoryRepository, clock)

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
            assertEquals(ImageTestStepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `selecting a distractor is Incorrect`() =
        runTest {
            val response = useCase(request(selectedOption = "dog"))
            assertEquals(ImageTestStepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `skip is Skipped regardless of selection`() =
        runTest {
            val response = useCase(request(selectedOption = null, skipped = true))
            assertEquals(ImageTestStepOutcome.SKIPPED, response.outcome)
        }
}
