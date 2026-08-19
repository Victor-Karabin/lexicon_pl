package com.lexicon.domain.passage

import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.dictation.AnswerNormalizer
import com.lexicon.interactors.passage.SubmitPassageAnswersRequest
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.interactors.training.RecordedAnswer
import com.lexicon.model.training.StepOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitPassageAnswersUseCaseImplTest {
    private val vocabulary: VocabularyRepository = mockk()
    private val recordAnswer: RecordAnswerUseCase = mockk(relaxed = true)
    private val useCase = SubmitPassageAnswersUseCaseImpl(vocabulary, recordAnswer, AnswerNormalizer())

    private val book = VocabularyItemBoundary(7, "książka", "a book", "ˈkʂɔ̃ʂka")

    @Test
    fun `an inflected gap is recorded against the starred word it came from`() =
        runTest {
            coEvery { vocabulary.findWordByText("książka") } returns book

            val response = useCase(
                SubmitPassageAnswersRequest(
                    sessionId = "s",
                    expected = listOf("książkę"),
                    answers = listOf("książkę"),
                    words = listOf("książka"),
                ),
            )

            assertEquals(listOf(true), response.correct)
            assertEquals("a book", response.results.single().translation)
            coVerify {
                recordAnswer(
                    match<RecordedAnswer> {
                        it.vocabularyItemId == 7L && it.outcome == StepOutcome.CORRECT
                    },
                )
            }
        }

    @Test
    fun `a wrong answer is reported and recorded as incorrect`() =
        runTest {
            coEvery { vocabulary.findWordByText("książka") } returns book

            val response = useCase(
                SubmitPassageAnswersRequest(
                    sessionId = "s",
                    expected = listOf("książkę"),
                    answers = listOf("gazetę"),
                    words = listOf("książka"),
                ),
            )

            assertEquals(listOf(false), response.correct)
            assertEquals("książkę", response.results.single().expected)
            assertEquals("gazetę", response.results.single().submitted)
            coVerify {
                recordAnswer(
                    match<RecordedAnswer> { it.outcome == StepOutcome.INCORRECT },
                )
            }
        }

    @Test
    fun `a gap whose word is no longer in the vocabulary still gets a result`() =
        runTest {
            coEvery { vocabulary.findWordByText(any()) } returns null

            val response = useCase(
                SubmitPassageAnswersRequest(
                    sessionId = "s",
                    expected = listOf("książkę"),
                    answers = listOf("książkę"),
                    words = listOf("książka"),
                ),
            )

            assertEquals(1, response.results.size)
            assertTrue(response.results.single().isCorrect)
            assertEquals("", response.results.single().translation)
        }

    @Test
    fun `an unanswered gap is still reported so the result screen lists every one`() =
        runTest {
            coEvery { vocabulary.findWordByText(any()) } returns book

            val response = useCase(
                SubmitPassageAnswersRequest(
                    sessionId = "s",
                    expected = listOf("książkę", "okno"),
                    answers = listOf("książkę"),
                    words = listOf("książka", "okno"),
                ),
            )

            assertEquals(2, response.results.size)
            assertEquals(listOf(true, false), response.correct)
        }
}
