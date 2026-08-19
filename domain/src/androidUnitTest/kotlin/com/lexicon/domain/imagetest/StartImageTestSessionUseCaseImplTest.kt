package com.lexicon.domain.imagetest

import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.domain.training.FakeSessionStore
import com.lexicon.interactors.imagetest.StartImageTestSessionRequest
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.coVerify
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
    private val useCase = StartImageTestSessionUseCaseImpl(vocabularyRepository, imageProvider, stepCountResolver, FakeSessionStore())

    private val items =
        listOf(
            Word(VocabularyId(1), "kot", "cat", "kɔt"),
            Word(VocabularyId(2), "pies", "dog", "pjɛs"),
            Word(VocabularyId(3), "dom", "house", "dɔm"),
            Word(VocabularyId(4), "woda", "water", "ˈvɔda"),
            Word(VocabularyId(5), "chleb", "bread", "xlɛp"),
            Word(VocabularyId(6), "książka", "book", "ˈkʂɔ̃ʐka"),
        )

    @Test
    fun `each step has exactly one correct option matching the subject's target word`() =
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
    fun `options never include a duplicate of the correct answer as a distractor`() =
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
    fun `distractors never mix single-word and phrase options with the correct answer`() =
        runTest {
            val mixedItems =
                items + listOf(
                    Word(VocabularyId(7), "dzień dobry", "good morning", "d͡ʑɛɲ ˈdɔbrɨ"),
                    Word(VocabularyId(8), "dobry wieczór", "good evening", "ˈdɔbrɨ ˈvjɛt͡ʂur"),
                )
            coEvery { vocabularyRepository.getRandomItems(any()) } returns mixedItems
            coEvery { imageProvider.searchImage(any()) } returns null

            val response = useCase(StartImageTestSessionRequest(stepCount = mixedItems.size, optionCount = 4))

            response.steps.forEach { step ->
                val correctIsPhrase = step.correctOption.contains(' ')
                step.options.forEach { option -> assertEquals(correctIsPhrase, option.contains(' ')) }
            }
        }

    @Test
    fun `every step is filled to the requested option count even though phrases are scarce`() =
        runTest {
            val scarcePhrases = listOf(
                Word(VocabularyId(90), "dzien dobry", "good morning", "d"),
                Word(VocabularyId(91), "gdzie jest dworzec", "where is the station", "g"),
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

    @Test
    fun `options are target-language words, not base-language ones`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns items
            coEvery { imageProvider.searchImage(any()) } returns "https://example.com/image.jpg"

            val response = useCase(StartImageTestSessionRequest(stepCount = 3, optionCount = 4))

            val targetWords = items.map { it.text }
            response.steps.forEach { step ->
                assertTrue("options must be target words, got ${step.options}", targetWords.containsAll(step.options))
                assertTrue(step.correctOption in targetWords)
                assertTrue("the clue is the base word", step.clueText in items.map { it.translation })
            }
        }

    @Test
    fun `images are still searched by the base word`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns items
            coEvery { imageProvider.searchImage(any()) } returns null
            val queries = mutableListOf<String>()

            useCase(StartImageTestSessionRequest(stepCount = 3, optionCount = 4))

            coVerify { imageProvider.searchImage(capture(queries)) }
            assertTrue(queries.all { it in items.map { item -> item.translation } })
        }
}
