package com.lexicon.pl.domain.puzzle

import com.lexicon.pl.boundary.TrainingHistoryRepository
import com.lexicon.pl.common.Clock
import com.lexicon.pl.domain.dictation.AnswerNormalizer
import com.lexicon.pl.interactors.puzzle.PuzzleStepOutcome
import com.lexicon.pl.interactors.puzzle.SubmitPuzzleAnswerRequest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitPuzzleAnswerUseCaseImplTest {
    private val trainingHistoryRepository: TrainingHistoryRepository = mockk(relaxed = true)
    private val clock: Clock = mockk { every { nowEpochMillis() } returns 1_000L }
    private val useCase = SubmitPuzzleAnswerUseCaseImpl(trainingHistoryRepository, AnswerNormalizer(), clock)

    private fun request(
        submittedText: String = "kot",
        tipUsed: Boolean = false,
        skipped: Boolean = false,
    ) = SubmitPuzzleAnswerRequest(
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
            assertEquals(PuzzleStepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `tip forces Incorrect even on a matching answer`() =
        runTest {
            val response = useCase(request(submittedText = "kot", tipUsed = true))
            assertEquals(PuzzleStepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `skip is Skipped regardless of tip`() =
        runTest {
            val response = useCase(request(skipped = true, tipUsed = true))
            assertEquals(PuzzleStepOutcome.SKIPPED, response.outcome)
        }
}
