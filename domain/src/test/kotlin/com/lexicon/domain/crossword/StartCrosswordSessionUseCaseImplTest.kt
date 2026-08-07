package com.lexicon.domain.crossword

import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.crossword.StartCrosswordSessionRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartCrosswordSessionUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk()
    private val imageProvider: ImageProvider = mockk()
    private val useCase = StartCrosswordSessionUseCaseImpl(vocabularyRepository, imageProvider)

    private val items = listOf(
        VocabularyItemBoundary(1, "kot", "cat", "kɔt"),
        VocabularyItemBoundary(2, "pies", "dog", "pjɛs"),
        VocabularyItemBoundary(3, "dom", "house", "dɔm"),
        VocabularyItemBoundary(4, "dzień dobry", "good morning", "d͡ʑɛɲ ˈdɔbrɨ"),
        VocabularyItemBoundary(5, "gdzie jest dworzec?", "where is the station?", "ɡd͡ʑɛ jɛst ˈdvɔʐɛt͡s"),
    )

    @Test
    fun `phrases are excluded from the puzzle`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns items
            coEvery { imageProvider.searchImage(any()) } returns null

            val response = useCase(StartCrosswordSessionRequest(wordCount = 5))

            assertTrue(response.words.none { it.expectedText.contains(' ') })
            assertEquals(setOf(1L, 2L, 3L), response.words.map { it.vocabularyItemId }.toSet())
        }

    @Test
    fun `only up to wordCount words are placed`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns items
            coEvery { imageProvider.searchImage(any()) } returns null

            val response = useCase(StartCrosswordSessionRequest(wordCount = 2))

            assertEquals(2, response.words.size)
        }

    @Test
    fun `every placed word carries an image url when the provider finds one`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns items
            coEvery { imageProvider.searchImage(any()) } returns "https://example.com/image.jpg"

            val response = useCase(StartCrosswordSessionRequest(wordCount = 3))

            assertTrue(response.words.all { it.imageUrl == "https://example.com/image.jpg" })
        }
}
