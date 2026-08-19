package com.lexicon.domain.presets

import com.lexicon.boundary.VocabularyPresetBoundary
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StudySetUseCasesImplTest {
    private val foodPreset = VocabularyPresetBoundary(
        id = "food",
        categoryId = "everyday-life",
        title = mapOf("en" to "Food"),
        description = emptyMap(),
        icon = null,
        color = null,
        popularity = 1,
        estimatedSeconds = 180,
        vocabularyIds = listOf(1L, 2L, 3L),
    )

    private val presetRepository: VocabularyPresetRepository = mockk {
        coEvery { getPreset("food") } returns foodPreset
        coEvery { getPreset("missing") } returns null
    }
    private val vocabularyRepository: VocabularyRepository = mockk(relaxed = true)

    @Test
    fun `toggling one word writes only that word`() =
        runTest {
            ToggleWordInStudySetUseCaseImpl(vocabularyRepository)(VocabularyId(7L), isInStudySet = true)

            coVerify { vocabularyRepository.setInStudySet(listOf(7L), true) }
        }

    @Test
    fun `taking a word out of the study set writes the flag off rather than deleting anything`() =
        runTest {
            ToggleWordInStudySetUseCaseImpl(vocabularyRepository)(VocabularyId(7L), isInStudySet = false)

            coVerify { vocabularyRepository.setInStudySet(listOf(7L), false) }
        }

    @Test
    fun `adding a preset writes all of its words in a single call`() =
        runTest {
            SetPresetInStudySetUseCaseImpl(presetRepository, vocabularyRepository)(PresetId("food"), true)

            coVerify(exactly = 1) { vocabularyRepository.setInStudySet(listOf(1L, 2L, 3L), true) }
        }

    @Test
    fun `removing a preset clears all of its words`() =
        runTest {
            SetPresetInStudySetUseCaseImpl(presetRepository, vocabularyRepository)(PresetId("food"), false)

            coVerify { vocabularyRepository.setInStudySet(listOf(1L, 2L, 3L), false) }
        }

    @Test
    fun `an unknown preset changes nothing instead of failing`() =
        runTest {
            SetPresetInStudySetUseCaseImpl(presetRepository, vocabularyRepository)(PresetId("missing"), true)

            coVerify(exactly = 0) { vocabularyRepository.setInStudySet(any(), any()) }
        }

    @Test
    fun `observed studySet arrive as typed ids`() =
        runTest {
            every { vocabularyRepository.observeStudySetIds() } returns flowOf(setOf(1L, 5L))

            val ids = ObserveStudySetIdsUseCaseImpl(vocabularyRepository)().first()

            assertEquals(setOf(VocabularyId(1L), VocabularyId(5L)), ids)
        }
}
