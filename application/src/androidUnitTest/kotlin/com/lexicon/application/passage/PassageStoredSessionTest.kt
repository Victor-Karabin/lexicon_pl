package com.lexicon.application.passage

import com.lexicon.application.dictation.AnswerNormalizer
import com.lexicon.application.training.FakeSessionStore
import com.lexicon.application.training.open
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.passage.SubmitPassageAnswersRequest
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

class PassageStoredSessionTest {
    private val vocabulary: VocabularyRepository = mockk(relaxed = true)
    private val recordAnswer: RecordAnswerUseCase = mockk(relaxed = true)
    private val sessions = FakeSessionStore()
    private val useCase = SubmitPassageAnswersUseCaseImpl(vocabulary, recordAnswer, AnswerNormalizer(), sessions)

    private suspend fun storedSession() =
        sessions.open(
            TrainingType.PASSAGE_BANK,
            listOf(VocabularyId(7) to "książkę", VocabularyId(8) to "okno"),
        )

    private fun request(
        sessionId: String,
        answers: List<String> = listOf("książkę", "okno"),
    ) = SubmitPassageAnswersRequest(
        sessionId = sessionId,
        expected = listOf("tampered one", "tampered two"),
        answers = answers,
        words = listOf("tampered word one", "tampered word two"),
    )

    @Test
    fun `the gaps are graded against the answers the session stored, not the caller's`() =
        runTest {
            val id = storedSession()

            val response = useCase(request(id.value))

            assertEquals(listOf(true, true), response.correct)
            assertEquals(listOf("książkę", "okno"), response.results.map { it.expected })
        }

    @Test
    fun `each gap is recorded against the word the session stored and under the session's training`() =
        runTest {
            val id = storedSession()

            useCase(request(id.value, answers = listOf("książkę", "x")))

            coVerify(exactly = 1) {
                recordAnswer(
                    match<RecordedAnswer> {
                        it.vocabularyItemId == 7L && it.stepIndex == 0 && it.expectedAnswer == "książkę" &&
                            it.outcome == StepOutcome.CORRECT && it.trainingType == TrainingType.PASSAGE_BANK
                    },
                )
            }
            coVerify(exactly = 1) {
                recordAnswer(
                    match<RecordedAnswer> {
                        it.vocabularyItemId == 8L && it.stepIndex == 1 && it.expectedAnswer == "okno" && it.outcome == StepOutcome.INCORRECT
                    },
                )
            }
        }

    @Test
    fun `submitting the same passage twice records every gap once`() =
        runTest {
            val id = storedSession()

            val first = useCase(request(id.value))
            val second = useCase(request(id.value))

            coVerify(exactly = 2) { recordAnswer(any()) }
            assertEquals(first.correct, second.correct)
        }
}
