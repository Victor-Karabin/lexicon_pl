package com.lexicon.application.dictationpuzzle

import com.lexicon.application.dictation.AnswerNormalizer
import com.lexicon.application.training.FakeSessionStore
import com.lexicon.application.training.open
import com.lexicon.interactors.dictationpuzzle.SubmitDictationPuzzleAnswerRequest
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitDictationPuzzleAnswerUseCaseImplTest {
    private val recordAnswer: RecordAnswerUseCase = mockk(relaxed = true)
    private val sessions = FakeSessionStore()
    private val useCase = SubmitDictationPuzzleAnswerUseCaseImpl(recordAnswer, AnswerNormalizer(), sessions)

    private fun request(
        submittedText: String = "kot",
        tipUsed: Boolean = false,
        skipped: Boolean = false,
        sessionId: String = "session-1",
        expectedText: String = "kot",
        vocabularyItemId: Long = 1L,
    ) = SubmitDictationPuzzleAnswerRequest(
        sessionId = sessionId,
        stepIndex = 0,
        vocabularyItemId = vocabularyItemId,
        expectedText = expectedText,
        submittedText = submittedText,
        tipUsed = tipUsed,
        skipped = skipped,
    )

    @Test
    fun `matching tiles without tip is Correct`() =
        runTest {
            val response = useCase(request(submittedText = "kot"))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `matching tiles with tip used is still Correct — tip usage doesn't affect the outcome`() =
        runTest {
            val response = useCase(request(submittedText = "kot", tipUsed = true))
            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `tip usage is recorded to history regardless of outcome`() =
        runTest {
            useCase(request(submittedText = "kot", tipUsed = true))

            coVerify {
                recordAnswer(
                    match<RecordedAnswer> { it.tipUsed },
                )
            }
        }

    @Test
    fun `skip is Skipped regardless of tip`() =
        runTest {
            val response = useCase(request(skipped = true, tipUsed = true))
            assertEquals(StepOutcome.SKIPPED, response.outcome)
        }

    @Test
    fun `case and surrounding whitespace do not matter`() =
        runTest {
            val response = useCase(request(expectedText = "Dzień dobry", submittedText = "  dzień dobry "))

            assertEquals(StepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `a missing question mark is wrong because punctuation counts`() =
        runTest {
            val response = useCase(request(expectedText = "a ty?", submittedText = "a ty"))

            assertEquals(StepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `punctuation in another place is wrong`() =
        runTest {
            val response = useCase(request(expectedText = "nie, dziękuję", submittedText = "nie dziękuję,"))

            assertEquals(StepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `the expected text and word come from the stored session, not from the caller`() =
        runTest {
            val sessionId = sessions.open(TrainingType.DICTATION_PUZZLE, listOf(VocabularyId(5L) to "kot")).value

            val response = useCase(request(sessionId = sessionId, expectedText = "pies", submittedText = "pies", vocabularyItemId = 9L))

            assertEquals(StepOutcome.INCORRECT, response.outcome)
            assertEquals("kot", response.expectedText)
            coVerify {
                recordAnswer(
                    RecordedAnswer(
                        sessionId = sessionId,
                        trainingType = TrainingType.DICTATION_PUZZLE,
                        stepIndex = 0,
                        vocabularyItemId = 5L,
                        expectedAnswer = "kot",
                        submittedAnswer = "pies",
                        outcome = StepOutcome.INCORRECT,
                    ),
                )
            }
        }

    @Test
    fun `the recorded answer keeps what was submitted and the tip flag`() =
        runTest {
            useCase(request(submittedText = "  KOT ", tipUsed = true))

            coVerify {
                recordAnswer(
                    RecordedAnswer(
                        sessionId = "session-1",
                        trainingType = TrainingType.DICTATION_PUZZLE,
                        stepIndex = 0,
                        vocabularyItemId = 1L,
                        expectedAnswer = "kot",
                        submittedAnswer = "  KOT ",
                        outcome = StepOutcome.CORRECT,
                        tipUsed = true,
                    ),
                )
            }
        }

    @Test
    fun `answering the same step twice records it to history once`() =
        runTest {
            val sessionId = sessions.open(TrainingType.DICTATION_PUZZLE, listOf(VocabularyId(1L) to "kot")).value

            useCase(request(sessionId = sessionId))
            useCase(request(sessionId = sessionId, submittedText = "pies"))

            coVerify(exactly = 1) { recordAnswer(any()) }
        }

    @Test
    fun `without a stored session the request's expected text and word are used and the answer is still recorded`() =
        runTest {
            val response = useCase(request(sessionId = "unknown", expectedText = "pies", submittedText = "pies", vocabularyItemId = 9L))

            assertEquals(StepOutcome.CORRECT, response.outcome)
            assertEquals("pies", response.expectedText)
            coVerify(exactly = 1) {
                recordAnswer(
                    match<RecordedAnswer> {
                        it.sessionId == "unknown" && it.vocabularyItemId == 9L && it.expectedAnswer == "pies"
                    },
                )
            }
        }
}
