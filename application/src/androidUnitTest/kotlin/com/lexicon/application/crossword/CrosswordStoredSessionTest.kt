package com.lexicon.application.crossword

import com.lexicon.application.dictation.AnswerNormalizer
import com.lexicon.application.training.FakeSessionStore
import com.lexicon.application.training.open
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.crossword.CrosswordWordSubmission
import com.lexicon.interactors.crossword.StartCrosswordSessionRequest
import com.lexicon.interactors.crossword.SubmitCrosswordRequest
import com.lexicon.interactors.training.RecordAnswerUseCase
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.Step
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CrosswordStoredSessionTest {
    private val recordAnswer: RecordAnswerUseCase = mockk(relaxed = true)
    private val sessions = FakeSessionStore()
    private val submit = SubmitCrosswordUseCaseImpl(recordAnswer = recordAnswer, answerNormalizer = AnswerNormalizer(), sessions = sessions)

    private val vocabulary: VocabularyRepository = mockk()
    private val start = StartCrosswordSessionUseCaseImpl(vocabularyRepository = vocabulary, sessions = sessions)

    @Test
    fun `submitting the same crossword twice records every word once`() =
        runTest {
            val id = sessions.open(TrainingType.CROSSWORD, listOf(VocabularyId(1) to "kot", VocabularyId(2) to "pies"))
            val request = SubmitCrosswordRequest(
                sessionId = id.value,
                words = listOf(
                    CrosswordWordSubmission(1, "kot", "kot", tipUsed = false),
                    CrosswordWordSubmission(2, "pies", "pies", tipUsed = false),
                ),
            )

            val first = submit(request)
            val second = submit(request)

            coVerify(exactly = 2) { recordAnswer(any()) }
            assertEquals(first.isFullyCorrect, second.isFullyCorrect)
        }

    @Test
    fun `a started crossword is stored with a step for every word placed`() =
        runTest {
            coEvery { vocabulary.getRandomItems(any(), any()) } returns listOf(
                Word(VocabularyId(1), "kot", "cat", "kɔt"),
                Word(VocabularyId(2), "tor", "track", "tɔr"),
                Word(VocabularyId(3), "dom", "house", "dɔm"),
            )

            val response = start(StartCrosswordSessionRequest(wordCount = 3))

            val stored = checkNotNull(sessions.find(SessionId(response.sessionId)))
            assertEquals(TrainingType.CROSSWORD, stored.training)
            assertEquals(response.words.map { it.vocabularyItemId }, stored.steps.map { (it as Step.Question).wordId.value })
            assertEquals(response.words.map { it.expectedText }, stored.steps.map { (it as Step.Question).expectedAnswer })
        }
}
