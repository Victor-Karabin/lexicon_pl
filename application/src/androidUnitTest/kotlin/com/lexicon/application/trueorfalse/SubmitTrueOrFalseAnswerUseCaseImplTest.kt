package com.lexicon.application.trueorfalse

import com.lexicon.application.training.FakeSessionStore
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.trueorfalse.SubmitTrueOrFalseAnswerRequest
import com.lexicon.model.training.StepOutcome
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitTrueOrFalseAnswerUseCaseImplTest {
    private val recordAnswer: RecordAnswerUseCase = mockk(relaxed = true)
    private val useCase = SubmitTrueOrFalseAnswerUseCaseImpl(recordAnswer, FakeSessionStore())

    private fun request(
        isCorrect: Boolean,
        userAnsweredTrue: Boolean,
    ) = SubmitTrueOrFalseAnswerRequest(
        sessionId = "session-1",
        stepIndex = 0,
        vocabularyItemId = 1L,
        isDisplayedTranslationCorrect = isCorrect,
        userAnsweredTrue = userAnsweredTrue,
    )

    @Test
    fun `answering True when the pairing is correct is Correct`() =
        runTest {
            val response = useCase(request(isCorrect = true, userAnsweredTrue = true))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `answering False when the pairing is a distractor is Correct`() =
        runTest {
            val response = useCase(request(isCorrect = false, userAnsweredTrue = false))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `answering True when the pairing is a distractor is Incorrect`() =
        runTest {
            val response = useCase(request(isCorrect = false, userAnsweredTrue = true))
            assertEquals(StepOutcome.INCORRECT, response.outcome)
        }
}
