package com.lexicon.pl.domain.wordbuilder

import com.lexicon.pl.boundary.TrainingHistoryRepository
import com.lexicon.pl.common.Clock
import com.lexicon.pl.domain.dictation.AnswerNormalizer
import com.lexicon.pl.interactors.wordbuilder.SubmitWordBuilderAnswerRequest
import com.lexicon.pl.interactors.wordbuilder.WordBuilderStepOutcome
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitWordBuilderAnswerUseCaseImplTest {
    private val trainingHistoryRepository: TrainingHistoryRepository = mockk(relaxed = true)
    private val clock: Clock = mockk { every { nowEpochMillis() } returns 1_000L }
    private val useCase = SubmitWordBuilderAnswerUseCaseImpl(trainingHistoryRepository, AnswerNormalizer(), clock)

    private fun request(
        submittedText: String = "kot",
        tipUsed: Boolean = false,
        skipped: Boolean = false,
    ) = SubmitWordBuilderAnswerRequest(
        sessionId = "session-1",
        stepIndex = 0,
        vocabularyItemId = 1L,
        expectedText = "kot",
        submittedText = submittedText,
        tipUsed = tipUsed,
        skipped = skipped,
    )

    @Test
    fun `diacritics must match exactly`() =
        runTest {
            val useCaseLocal = SubmitWordBuilderAnswerUseCaseImpl(trainingHistoryRepository, AnswerNormalizer(), clock)
            val request = SubmitWordBuilderAnswerRequest("s", 0, 2L, "łódź", "lodz", tipUsed = false, skipped = false)
            val response = useCaseLocal(request)
            assertEquals(WordBuilderStepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `matching tiles without tip is Correct`() =
        runTest {
            val response = useCase(request(submittedText = "kot"))
            assertEquals(WordBuilderStepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `tip forces Incorrect even on a matching answer`() =
        runTest {
            val response = useCase(request(submittedText = "kot", tipUsed = true))
            assertEquals(WordBuilderStepOutcome.INCORRECT, response.outcome)
        }
}
