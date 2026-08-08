package com.lexicon.domain.imagetest

import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.imagetest.StartImageTestSessionRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartImageTestSessionUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk()
    private val imageProvider: ImageProvider = mockk()
    private val settingsRepository: SettingsRepository = mockk {
        coEvery { getSettings() } returns AppSettingsBoundary(ThemeModeBoundary.SYSTEM, stepCount = 10)
    }
    private val stepCountResolver = StepCountResolver(settingsRepository)
    private val useCase = StartImageTestSessionUseCaseImpl(vocabularyRepository, imageProvider, stepCountResolver)

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

    @Test
    fun `distractors never mix single-word and phrase translations with the correct answer`() =
        runTest {
            val mixedItems =
                items + listOf(
                    VocabularyItemBoundary(7, "dzień dobry", "good morning", "d͡ʑɛɲ ˈdɔbrɨ"),
                    VocabularyItemBoundary(8, "dobry wieczór", "good evening", "ˈdɔbrɨ ˈvjɛt͡ʂur"),
                )
            coEvery { vocabularyRepository.getRandomItems(any()) } returns mixedItems
            coEvery { imageProvider.searchImage(any()) } returns null

            val response = useCase(StartImageTestSessionRequest(stepCount = mixedItems.size, optionCount = 4))

            response.steps.forEach { step ->
                val correctIsPhrase = step.correctOption.contains(' ')
                step.options.forEach { option -> assertEquals(correctIsPhrase, option.contains(' ')) }
            }
        }

    /**
     * Regression: with only a couple of phrases in the vocabulary, a phrase subject had no
     * same-type distractors left and the step degenerated to a single option.
     */
    @Test
    fun `every step is filled to the requested option count even though phrases are scarce`() =
        runTest {
            // Phrases first, so the naive "take the first item" subject choice lands on one.
            val scarcePhrases = listOf(
                VocabularyItemBoundary(90, "dzien dobry", "good morning", "d"),
                VocabularyItemBoundary(91, "gdzie jest dworzec", "where is the station", "g"),
            ) + items
            coEvery { vocabularyRepository.getRandomItems(any()) } returns scarcePhrases
            coEvery { imageProvider.searchImage(any()) } returns null

            repeat(5) {
                val response = useCase(StartImageTestSessionRequest(stepCount = 1, optionCount = 6))

                response.steps.forEach { step ->
                    assertEquals("a one-option step is trivially guessable", 6, step.options.size)
                }
            }
        }
}
