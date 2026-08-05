package com.lexicon.pl.domain.imagetest

import com.lexicon.pl.boundary.ImageProvider
import com.lexicon.pl.boundary.VocabularyItemBoundary
import com.lexicon.pl.boundary.VocabularyRepository
import com.lexicon.pl.interactors.imagetest.StartImageTestSessionRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartImageTestSessionUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk()
    private val imageProvider: ImageProvider = mockk()
    private val useCase = StartImageTestSessionUseCaseImpl(vocabularyRepository, imageProvider)

    private val items =
        listOf(
            VocabularyItemBoundary(1, "kot", "cat", "kɔt"),
            VocabularyItemBoundary(2, "pies", "dog", "pjɛs"),
            VocabularyItemBoundary(3, "dom", "house", "dɔm"),
            VocabularyItemBoundary(4, "woda", "water", "ˈvɔda"),
            VocabularyItemBoundary(5, "chleb", "bread", "xlɛp"),
            VocabularyItemBoundary(6, "książka", "book", "ˈkʂɔ̃ʐka"),
        )

    @Test
    fun `each step has exactly one correct option matching the subject's translation`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns items
            coEvery { imageProvider.searchImage(any()) } returns "https://example.com/image.jpg"

            val response = useCase(StartImageTestSessionRequest(stepCount = 3, optionCount = 4))

            response.steps.forEach { step ->
                assertTrue(step.options.contains(step.correctOption))
                assertEquals(1, step.options.count { it == step.correctOption })
            }
        }

    @Test
    fun `options never include a duplicate of the correct translation as a distractor`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns items
            coEvery { imageProvider.searchImage(any()) } returns null

            val response = useCase(StartImageTestSessionRequest(stepCount = 3, optionCount = 4))

            response.steps.forEach { step ->
                val distractors = step.options.filterNot { it == step.correctOption }
                assertTrue(distractors.none { it == step.correctOption })
            }
        }
}
