package com.lexicon.domain.presets

import com.lexicon.boundary.PresetCategoryBoundary
import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyPresetBoundary
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.CefrLevel
import com.lexicon.interactors.presets.PresetId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class VocabularyPresetUseCasesImplTest {
    private fun preset(
        id: String,
        categoryId: String = "everyday-life",
        cefr: String? = null,
        popularity: Int = 1,
        vocabularyIds: List<Long> = listOf(3L, 1L, 2L),
    ) = VocabularyPresetBoundary(
        id = id,
        categoryId = categoryId,
        title = mapOf("en" to id),
        description = mapOf("en" to ""),
        icon = null,
        color = null,
        cefr = cefr,
        popularity = popularity,
        estimatedSeconds = 180,
        vocabularyIds = vocabularyIds,
    )

    private val categories = listOf(
        PresetCategoryBoundary("everyday-life", 3, mapOf("en" to "Everyday life")),
        PresetCategoryBoundary("nature", 5, mapOf("en" to "Nature")),
    )

    private val presetRepository: VocabularyPresetRepository = mockk {
        coEvery { getCategories() } returns categories
        coEvery { getPresets() } returns listOf(
            preset("birds", categoryId = "nature", popularity = 2),
            preset("food", popularity = 9),
            preset("greetings", popularity = 4),
        )
        coEvery { getPreset("food") } returns preset("food")
        coEvery { getPreset("missing") } returns null
    }

    private val vocabularyRepository: VocabularyRepository = mockk {
        coEvery { getItemsByIds(any()) } returns listOf(
            VocabularyItemBoundary(1L, "kot", "cat", "kɔt"),
            VocabularyItemBoundary(2L, "pies", "dog", "pjɛs"),
            VocabularyItemBoundary(3L, "dom", "house", "dɔm"),
        )
    }

    @Test
    fun `presets are grouped by category order, then by popularity within it`() =
        runTest {
            val presets = GetVocabularyPresetsUseCaseImpl(presetRepository)()

            assertEquals(listOf("greetings", "food", "birds"), presets.map { it.id.value })
        }

    /**
     * A preset whose category is missing cannot be placed in the browser at all; the
     * validator reports it as a data fault, and showing it half-placed would only mask that.
     */
    @Test
    fun `a preset naming an undefined category is dropped rather than shown uncategorised`() =
        runTest {
            coEvery { presetRepository.getPresets() } returns listOf(preset("orphan", categoryId = "invented"))

            assertTrue(GetVocabularyPresetsUseCaseImpl(presetRepository)().isEmpty())
        }

    @Test
    fun `a CEFR value the app does not know degrades to null instead of failing`() =
        runTest {
            coEvery { presetRepository.getPresets() } returns listOf(preset("future", cefr = "D3"))

            assertNull(GetVocabularyPresetsUseCaseImpl(presetRepository)().single().cefr)
        }

    @Test
    fun `a recognised CEFR value is carried through`() =
        runTest {
            coEvery { presetRepository.getPresets() } returns listOf(preset("a1", cefr = "A1"))

            assertEquals(CefrLevel.A1, GetVocabularyPresetsUseCaseImpl(presetRepository)().single().cefr)
        }

    @Test
    fun `estimated duration is carried across as a real duration`() =
        runTest {
            val preset = GetVocabularyPresetsUseCaseImpl(presetRepository)().first()

            assertEquals(180.seconds, preset.estimatedDuration)
        }

    @Test
    fun `an unknown preset id resolves to null rather than an empty preset`() =
        runTest {
            assertNull(GetVocabularyPresetUseCaseImpl(presetRepository)(PresetId("missing")))
        }

    /** "100 most common words" is only meaningful if it arrives in frequency order. */
    @Test
    fun `preset vocabulary follows the preset's own order, not the store's`() =
        runTest {
            val useCase = GetPresetVocabularyUseCaseImpl(presetRepository, vocabularyRepository)

            val words = useCase(PresetId("food"))

            assertEquals(listOf("dom", "kot", "pies"), words.map { it.text })
        }

    @Test
    fun `words the store no longer holds are skipped, leaving the rest usable`() =
        runTest {
            coEvery { presetRepository.getPreset("food") } returns preset("food", vocabularyIds = listOf(1L, 404L, 2L))
            val useCase = GetPresetVocabularyUseCaseImpl(presetRepository, vocabularyRepository)

            assertEquals(listOf("kot", "pies"), useCase(PresetId("food")).map { it.text })
        }

    @Test
    fun `an unknown preset yields no words instead of throwing`() =
        runTest {
            val useCase = GetPresetVocabularyUseCaseImpl(presetRepository, vocabularyRepository)

            assertTrue(useCase(PresetId("missing")).isEmpty())
        }

    @Test
    fun `categories come back in their declared order`() =
        runTest {
            val ordered = GetPresetCategoriesUseCaseImpl(presetRepository)()

            assertEquals(listOf("everyday-life", "nature"), ordered.map { it.id })
        }
}
