package com.lexicon.application.fillword

import com.lexicon.application.training.FakeSessionStore
import com.lexicon.application.training.open
import com.lexicon.interactors.fillword.SubmitFillwordRequest
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SubmitFillwordUseCaseImplTest {
    private val recordAnswer: RecordAnswerUseCase = mockk(relaxed = true)
    private val sessions = FakeSessionStore()
    private val submit = SubmitFillwordUseCaseImpl(recordAnswer = recordAnswer, sessions = sessions)

    private suspend fun storedRound() =
        sessions.open(
            TrainingType.FILLWORD,
            listOf(VocabularyId(7) to "kot", VocabularyId(8) to "pies", VocabularyId(9) to "stół"),
        )

    @Test
    fun `a found word is recorded as correct against the stored word and its step`() =
        runTest {
            val id = storedRound()

            submit(SubmitFillwordRequest(sessionId = id.value, foundWords = setOf("PIES", "STÓŁ")))

            coVerify(exactly = 1) {
                recordAnswer(
                    RecordedAnswer(
                        sessionId = id.value,
                        trainingType = TrainingType.FILLWORD,
                        stepIndex = 1,
                        vocabularyItemId = 8L,
                        expectedAnswer = "pies",
                        submittedAnswer = "pies",
                        outcome = StepOutcome.CORRECT,
                        tipUsed = false,
                    ),
                )
            }
            coVerify(exactly = 1) {
                recordAnswer(
                    match<RecordedAnswer> {
                        it.stepIndex == 2 && it.vocabularyItemId == 9L && it.expectedAnswer == "stół" &&
                            it.submittedAnswer == "stół" && it.outcome == StepOutcome.CORRECT
                    },
                )
            }
        }

    @Test
    fun `a word that was not found is recorded as incorrect with nothing submitted`() =
        runTest {
            val id = storedRound()

            submit(SubmitFillwordRequest(sessionId = id.value, foundWords = setOf("PIES")))

            coVerify(exactly = 1) {
                recordAnswer(
                    RecordedAnswer(
                        sessionId = id.value,
                        trainingType = TrainingType.FILLWORD,
                        stepIndex = 0,
                        vocabularyItemId = 7L,
                        expectedAnswer = "kot",
                        submittedAnswer = "",
                        outcome = StepOutcome.INCORRECT,
                        tipUsed = false,
                    ),
                )
            }
            coVerify(exactly = 1) {
                recordAnswer(match<RecordedAnswer> { it.stepIndex == 2 && it.outcome == StepOutcome.INCORRECT && it.submittedAnswer == "" })
            }
        }

    @Test
    fun `every stored word is recorded exactly once per round`() =
        runTest {
            val id = storedRound()

            submit(SubmitFillwordRequest(sessionId = id.value, foundWords = setOf("KOT")))

            coVerify(exactly = 3) { recordAnswer(any()) }
        }

    @Test
    fun `submitting the same round twice records every word once`() =
        runTest {
            val id = storedRound()
            val request = SubmitFillwordRequest(sessionId = id.value, foundWords = setOf("KOT", "PIES"))

            submit(request)
            submit(request)

            coVerify(exactly = 3) { recordAnswer(any()) }
        }

    @Test
    fun `a session that was never stored records nothing`() =
        runTest {
            submit(SubmitFillwordRequest(sessionId = "unknown", foundWords = setOf("KOT")))

            coVerify(exactly = 0) { recordAnswer(any()) }
        }

    @Test
    fun `found entries that are not stored words are ignored`() =
        runTest {
            val id = storedRound()

            submit(SubmitFillwordRequest(sessionId = id.value, foundWords = setOf("KOT", "TAMPERED")))

            coVerify(exactly = 3) { recordAnswer(any()) }
            coVerify(exactly = 1) { recordAnswer(match<RecordedAnswer> { it.stepIndex == 0 && it.outcome == StepOutcome.CORRECT }) }
            coVerify(exactly = 2) { recordAnswer(match<RecordedAnswer> { it.outcome == StepOutcome.INCORRECT }) }
        }
}
