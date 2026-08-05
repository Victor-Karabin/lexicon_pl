package com.lexicon.pl.domain.dictation

import com.lexicon.pl.boundary.VocabularyItemBoundary
import com.lexicon.pl.boundary.VocabularyRepository
import com.lexicon.pl.interactors.dictation.StartDictationSessionRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StartDictationSessionUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk()
    private val useCase = StartDictationSessionUseCaseImpl(vocabularyRepository)

    @Test
    fun `builds one step per returned vocabulary item, preserving order`() =
        runTest {
            val items =
                listOf(
                    VocabularyItemBoundary(1, "kot", "cat", "kɔt"),
                    VocabularyItemBoundary(2, "pies", "dog", "pjɛs"),
                )
            coEvery { vocabularyRepository.getRandomItems(2) } returns items

            val response = useCase(StartDictationSessionRequest(stepCount = 2))

            assertEquals(2, response.steps.size)
            assertEquals(0, response.steps[0].stepIndex)
            assertEquals(1L, response.steps[0].vocabularyItemId)
            assertEquals("kot", response.steps[0].expectedText)
            assertEquals(1, response.steps[1].stepIndex)
            assertEquals("pies", response.steps[1].expectedText)
        }

    @Test
    fun `requests the configured step count from the repository`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns emptyList()

            useCase(StartDictationSessionRequest(stepCount = 7))

            io.mockk.coVerify { vocabularyRepository.getRandomItems(7) }
        }
}
