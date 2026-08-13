package com.lexicon.domain.trueorfalse

import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.trueorfalse.StartTrueOrFalseSessionRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartTrueOrFalseSessionUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk()
    private val useCase = StartTrueOrFalseSessionUseCaseImpl(vocabularyRepository)

    private val items =
        listOf(
            VocabularyItemBoundary(1, "kot", "cat", "kɔt"),
            VocabularyItemBoundary(2, "pies", "dog", "pjɛs"),
            VocabularyItemBoundary(3, "dom", "house", "dɔm"),
            VocabularyItemBoundary(4, "woda", "water", "ˈvɔda"),
        )

    @Test
    fun `probability 1 always shows the correct translation`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns items
            val response = useCase(StartTrueOrFalseSessionRequest(poolSize = 2, correctProbability = 1.0))
            response.steps.forEach { step ->
                assertTrue(step.isDisplayedTranslationCorrect)
                assertEquals(items.first { it.id == step.vocabularyItemId }.translation, step.displayedTranslation)
            }
        }

    @Test
    fun `probability 0 always shows a distractor translation`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns items
            val response = useCase(StartTrueOrFalseSessionRequest(poolSize = 2, correctProbability = 0.0))
            response.steps.forEach { step ->
                val subject = items.first { it.id == step.vocabularyItemId }
                assertEquals(false, step.isDisplayedTranslationCorrect)
                assertTrue(step.displayedTranslation != subject.translation)
            }
        }

    @Test
    fun `isDisplayedTranslationCorrect always matches whether the shown text is the subject's own translation`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns items
            val response = useCase(StartTrueOrFalseSessionRequest(poolSize = 4, correctProbability = 0.5))
            response.steps.forEach { step ->
                val subject = items.first { it.id == step.vocabularyItemId }
                assertEquals(step.displayedTranslation == subject.translation, step.isDisplayedTranslationCorrect)
            }
        }

    @Test
    fun `a distractor translation never mixes a single word with a phrase`() =
        runTest {
            val mixedItems =
                items + listOf(
                    VocabularyItemBoundary(5, "dzień dobry", "good morning", "d͡ʑɛɲ ˈdɔbrɨ"),
                    VocabularyItemBoundary(6, "dobry wieczór", "good evening", "ˈdɔbrɨ ˈvjɛt͡ʂur"),
                )
            coEvery { vocabularyRepository.getRandomItems(any()) } returns mixedItems
            val response = useCase(StartTrueOrFalseSessionRequest(poolSize = mixedItems.size, correctProbability = 0.0))
            response.steps.forEach { step ->
                val subject = mixedItems.first { it.id == step.vocabularyItemId }
                assertEquals(subject.translation.contains(' '), step.displayedTranslation.contains(' '))
            }
        }
}
