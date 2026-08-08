package com.lexicon.domain.presets

import com.lexicon.boundary.VocabularyPresetBoundary
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.VocabularyId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FavouriteUseCasesImplTest {
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
            ToggleWordFavouriteUseCaseImpl(vocabularyRepository)(VocabularyId(7L), isFavourite = true)

            coVerify { vocabularyRepository.setFavourite(listOf(7L), true) }
        }

    @Test
    fun `un-favouriting a word writes the flag off rather than deleting anything`() =
        runTest {
            ToggleWordFavouriteUseCaseImpl(vocabularyRepository)(VocabularyId(7L), isFavourite = false)

            coVerify { vocabularyRepository.setFavourite(listOf(7L), false) }
        }

    /**
     * One write, not one per word: a thousand-word preset would otherwise emit a thousand
     * updates and make every observer redraw on each.
     */
    @Test
    fun `favouriting a preset writes all of its words in a single call`() =
        runTest {
            SetPresetFavouriteUseCaseImpl(presetRepository, vocabularyRepository)(PresetId("food"), true)

            coVerify(exactly = 1) { vocabularyRepository.setFavourite(listOf(1L, 2L, 3L), true) }
        }

    @Test
    fun `un-favouriting a preset clears all of its words`() =
        runTest {
            SetPresetFavouriteUseCaseImpl(presetRepository, vocabularyRepository)(PresetId("food"), false)

            coVerify { vocabularyRepository.setFavourite(listOf(1L, 2L, 3L), false) }
        }

    @Test
    fun `an unknown preset changes nothing instead of failing`() =
        runTest {
            SetPresetFavouriteUseCaseImpl(presetRepository, vocabularyRepository)(PresetId("missing"), true)

            coVerify(exactly = 0) { vocabularyRepository.setFavourite(any(), any()) }
        }

    @Test
    fun `observed favourites arrive as typed ids`() =
        runTest {
            every { vocabularyRepository.observeFavouriteIds() } returns flowOf(setOf(1L, 5L))

            val ids = ObserveFavouriteWordIdsUseCaseImpl(vocabularyRepository)().first()

            assertEquals(setOf(VocabularyId(1L), VocabularyId(5L)), ids)
        }
}
