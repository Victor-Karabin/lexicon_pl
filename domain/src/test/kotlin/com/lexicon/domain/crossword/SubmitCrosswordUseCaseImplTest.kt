package com.lexicon.domain.crossword

import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.common.Clock
import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.interactors.crossword.CrosswordWordOutcome
import com.lexicon.interactors.crossword.CrosswordWordSubmission
import com.lexicon.interactors.crossword.SubmitCrosswordRequest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitCrosswordUseCaseImplTest {
    private val trainingHistoryRepository: TrainingHistoryRepository = mockk(relaxed = true)
    private val clock: Clock = mockk { every { nowEpochMillis() } returns 1_000L }
    private val useCase = SubmitCrosswordUseCaseImpl(trainingHistoryRepository, AnswerNormalizer(), clock)

    private fun submission(
        id: Long,
        expected: String,
        submitted: String,
        tipUsed: Boolean = false,
    ) = CrosswordWordSubmission(vocabularyItemId = id, expectedText = expected, submittedText = submitted, tipUsed = tipUsed)

    @Test
    fun `every word correct and no tips used is fully correct`() =
        runTest {
            val response = useCase(
                SubmitCrosswordRequest(
                    sessionId = "session-1",
                    words = listOf(submission(1, "kot", "kot"), submission(2, "pies", "pies")),
                ),
            )
            assertTrue(response.isFullyCorrect)
            assertTrue(response.wordResults.all { it.outcome == CrosswordWordOutcome.CORRECT })
        }

    @Test
    fun `one incorrect word makes the whole crossword incorrect`() =
        runTest {
            val response = useCase(
                SubmitCrosswordRequest(
                    sessionId = "session-1",
                    words = listOf(submission(1, "kot", "kot"), submission(2, "pies", "piess")),
                ),
            )
            assertFalse(response.isFullyCorrect)
            assertEquals(CrosswordWordOutcome.INCORRECT, response.wordResults[1].outcome)
        }

    @Test
    fun `every word spelled correctly but a tip was used is still incorrect overall`() =
        runTest {
            val response = useCase(
                SubmitCrosswordRequest(
                    sessionId = "session-1",
                    words = listOf(submission(1, "kot", "kot", tipUsed = true), submission(2, "pies", "pies")),
                ),
            )
            assertFalse(response.isFullyCorrect)
            assertTrue(response.wordResults.all { it.outcome == CrosswordWordOutcome.CORRECT })
        }

    @Test
    fun `an empty word list is not fully correct — vacuous truth would otherwise mark it complete`() =
        runTest {
            val response = useCase(SubmitCrosswordRequest(sessionId = "session-1", words = emptyList()))
            assertFalse(response.isFullyCorrect)
        }

    @Test
    fun `comparison is case-insensitive with whitespace trimmed`() =
        runTest {
            val response = useCase(
                SubmitCrosswordRequest(sessionId = "session-1", words = listOf(submission(1, "kot", " KOT "))),
            )
            assertEquals(CrosswordWordOutcome.CORRECT, response.wordResults.single().outcome)
        }
}
