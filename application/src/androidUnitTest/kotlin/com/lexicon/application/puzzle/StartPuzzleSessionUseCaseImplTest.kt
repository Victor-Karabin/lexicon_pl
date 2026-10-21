package com.lexicon.application.puzzle

import com.lexicon.application.training.FakeSessionStore
import com.lexicon.application.training.SETTINGS_STEP_COUNT
import com.lexicon.application.training.questions
import com.lexicon.application.training.sessionOf
import com.lexicon.application.training.sessionWords
import com.lexicon.application.training.stepCountResolver
import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.puzzle.StartPuzzleSessionRequest
import com.lexicon.model.training.Step
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartPuzzleSessionUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk()
    private val imageProvider: ImageProvider = mockk()
    private val sessions = FakeSessionStore()
    private val useCase = StartPuzzleSessionUseCaseImpl(vocabularyRepository, imageProvider, stepCountResolver(), sessions)

    @Test
    fun `builds one step per returned word with the translation as the clue`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any(), any()) } returns sessionWords
            coEvery { imageProvider.searchImage(any()) } returns null

            val response = useCase(StartPuzzleSessionRequest(stepCount = 2))

            assertEquals(listOf(0, 1), response.steps.map { it.stepIndex })
            assertEquals(listOf(1L, 2L), response.steps.map { it.vocabularyItemId })
            assertEquals(listOf("kot", "pies"), response.steps.map { it.expectedText })
            assertEquals(listOf("cat", "dog"), response.steps.map { it.clueText })
        }

    @Test
    fun `each step carries the image the provider finds for its word`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any(), any()) } returns sessionWords
            coEvery { imageProvider.searchImage("cat") } returns "https://example.com/cat.jpg"
            coEvery { imageProvider.searchImage("dog") } returns "https://example.com/dog.jpg"

            val response = useCase(StartPuzzleSessionRequest(stepCount = 2))

            assertEquals(
                listOf("https://example.com/cat.jpg", "https://example.com/dog.jpg"),
                response.steps.map { it.imageUrl },
            )
        }

    @Test
    fun `a failed image lookup leaves that step without a picture`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any(), any()) } returns sessionWords
            coEvery { imageProvider.searchImage("cat") } throws IllegalStateException("offline")
            coEvery { imageProvider.searchImage("dog") } returns "https://example.com/dog.jpg"

            val response = useCase(StartPuzzleSessionRequest(stepCount = 2))

            assertNull(response.steps[0].imageUrl)
            assertEquals("https://example.com/dog.jpg", response.steps[1].imageUrl)
            assertEquals(listOf("kot", "pies"), response.steps.map { it.expectedText })
        }

    @Test
    fun `stores the session as a puzzle holding each word's id and text`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any(), any()) } returns sessionWords
            coEvery { imageProvider.searchImage(any()) } returns null

            val response = useCase(StartPuzzleSessionRequest(stepCount = 2))

            val session = sessions.sessionOf(response.sessionId)!!
            assertEquals(TrainingType.PUZZLE, session.training)
            assertEquals(
                listOf(
                    Step.Question(index = 0, wordId = VocabularyId(1), expectedAnswer = "kot"),
                    Step.Question(index = 1, wordId = VocabularyId(2), expectedAnswer = "pies"),
                ),
                session.questions(),
            )
        }

    @Test
    fun `an explicit step count beats the setting`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any(), any()) } returns emptyList()

            useCase(StartPuzzleSessionRequest(stepCount = 3))

            coVerify { vocabularyRepository.getRandomItems(3, emptyList()) }
        }

    @Test
    fun `without an explicit step count the setting decides`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any(), any()) } returns emptyList()

            useCase(StartPuzzleSessionRequest())

            coVerify { vocabularyRepository.getRandomItems(SETTINGS_STEP_COUNT, emptyList()) }
        }

    @Test
    fun `the vocabulary ids reach the repository`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any(), any()) } returns emptyList()

            useCase(StartPuzzleSessionRequest(stepCount = 2, vocabularyIds = listOf(4L, 5L)))

            coVerify { vocabularyRepository.getRandomItems(2, listOf(4L, 5L)) }
        }

    @Test
    fun `no words means no steps and nothing stored`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any(), any()) } returns emptyList()

            val response = useCase(StartPuzzleSessionRequest(stepCount = 2))

            assertEquals(emptyList<Any>(), response.steps)
            assertNull(sessions.sessionOf(response.sessionId))
        }
}
