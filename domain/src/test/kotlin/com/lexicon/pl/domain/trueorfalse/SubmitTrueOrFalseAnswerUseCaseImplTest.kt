package com.lexicon.pl.domain.trueorfalse

import com.lexicon.pl.boundary.TrainingHistoryRepository
import com.lexicon.pl.common.Clock
import com.lexicon.pl.interactors.trueorfalse.SubmitTrueOrFalseAnswerRequest
import com.lexicon.pl.interactors.trueorfalse.TrueOrFalseStepOutcome
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitTrueOrFalseAnswerUseCaseImplTest {
    private val trainingHistoryRepository: TrainingHistoryRepository = mockk(relaxed = true)
    private val clock: Clock = mockk { every { nowEpochMillis() } returns 1_000L }
    private val useCase = SubmitTrueOrFalseAnswerUseCaseImpl(trainingHistoryRepository, clock)

    private fun request(
        isCorrect: Boolean,
        userAnsweredTrue: Boolean?,
        skipped: Boolean = false,
    ) = SubmitTrueOrFalseAnswerRequest(
        sessionId = "session-1",
        stepIndex = 0,
        vocabularyItemId = 1L,
        isDisplayedTranslationCorrect = isCorrect,
        userAnsweredTrue = userAnsweredTrue,
        skipped = skipped,
    )

    @Test
    fun `answering True when the pairing is correct is Correct`() =
        runTest {
            val response = useCase(request(isCorrect = true, userAnsweredTrue = true))
            assertEquals(TrueOrFalseStepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `answering False when the pairing is a distractor is Correct`() =
        runTest {
            val response = useCase(request(isCorrect = false, userAnsweredTrue = false))
            assertEquals(TrueOrFalseStepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `answering True when the pairing is a distractor is Incorrect`() =
        runTest {
            val response = useCase(request(isCorrect = false, userAnsweredTrue = true))
            assertEquals(TrueOrFalseStepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `skip is Skipped regardless of the pairing`() =
        runTest {
            val response = useCase(request(isCorrect = true, userAnsweredTrue = null, skipped = true))
            assertEquals(TrueOrFalseStepOutcome.SKIPPED, response.outcome)
        }
}
