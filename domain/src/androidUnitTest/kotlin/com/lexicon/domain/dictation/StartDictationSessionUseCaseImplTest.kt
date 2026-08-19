package com.lexicon.domain.dictation

import com.lexicon.boundary.AppSettingsBoundary
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.ThemeModeBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.domain.settings.StepCountResolver
import com.lexicon.interactors.dictation.StartDictationSessionRequest
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StartDictationSessionUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk()
    private val settingsRepository: SettingsRepository = mockk {
        coEvery { getSettings() } returns AppSettingsBoundary(ThemeModeBoundary.SYSTEM, stepCount = 10)
    }
    private val stepCountResolver = StepCountResolver(settingsRepository)
    private val useCase = StartDictationSessionUseCaseImpl(vocabularyRepository, stepCountResolver)

    @Test
    fun `builds one step per returned vocabulary item, preserving order`() =
        runTest {
            val items =
                listOf(
                    Word(VocabularyId(1), "kot", "cat", "kɔt"),
                    Word(VocabularyId(2), "pies", "dog", "pjɛs"),
                )
            coEvery { vocabularyRepository.getRandomItems(2) } returns items

            val response = useCase(StartDictationSessionRequest(stepCount = 2))

            assertEquals(2, response.steps.size)
            assertEquals(0, response.steps[0].stepIndex)
            assertEquals(1L, response.steps[0].vocabularyItemId)
            assertEquals("kot", response.steps[0].expectedText)
            assertEquals("cat", response.steps[0].translationText)
            assertEquals(1, response.steps[1].stepIndex)
            assertEquals("pies", response.steps[1].expectedText)
            assertEquals("dog", response.steps[1].translationText)
        }

    @Test
    fun `requests the configured step count from the repository`() =
        runTest {
            coEvery { vocabularyRepository.getRandomItems(any()) } returns emptyList()

            useCase(StartDictationSessionRequest(stepCount = 7))

            io.mockk.coVerify { vocabularyRepository.getRandomItems(7) }
        }
}
