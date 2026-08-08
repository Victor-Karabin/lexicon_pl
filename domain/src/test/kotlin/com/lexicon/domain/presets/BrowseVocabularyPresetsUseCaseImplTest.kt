package com.lexicon.domain.presets

import com.lexicon.interactors.presets.BrowsePresetsRequest
import com.lexicon.interactors.presets.GetVocabularyPresetsUseCase
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetSort
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class BrowseVocabularyPresetsUseCaseImplTest {
    private val everyday = PresetCategory("everyday-life", 3, LocalizedText(mapOf("en" to "Everyday life")))
    private val nature = PresetCategory("nature", 5, LocalizedText(mapOf("en" to "Nature")))

    private fun preset(
        id: String,
        title: String,
        category: PresetCategory = everyday,
        words: Int = 20,
        popularity: Int = 1,
        description: String = "",
        titlePl: String = title,
    ) = VocabularyPreset(
        id = PresetId(id),
        title = LocalizedText(mapOf("en" to title, "pl" to titlePl)),
        description = LocalizedText(mapOf("en" to description)),
        category = category,
        icon = null,
        color = null,
        popularity = popularity,
        estimatedDuration = words.minutes,
        vocabularyIds = List(words) { VocabularyId(it.toLong()) }.toImmutableList(),
    )

    private val catalog = listOf(
        preset("food", "Food", words = 58, popularity = 17, description = "Meals and ingredients."),
        preset("birds", "Birds", nature, words = 21, popularity = 37, titlePl = "Ptaki"),
        preset("greetings", "Greetings", words = 20, popularity = 5),
        preset("numbers", "Numbers", words = 90, popularity = 8),
        preset("zebra", "Zebra crossings", nature, words = 7, popularity = 99),
    )

    private val getPresets: GetVocabularyPresetsUseCase = mockk {
        coEvery { this@mockk() } returns catalog.toImmutableList()
    }
    private val useCase = BrowseVocabularyPresetsUseCaseImpl(getPresets)

    @Test
    fun `an empty request returns the whole catalogue`() =
        runTest {
            assertEquals(catalog.size, useCase(BrowsePresetsRequest()).size)
        }

    @Test
    fun `search matches the title`() =
        runTest {
            val results = useCase(BrowsePresetsRequest(query = "food"))

            assertEquals(listOf("food"), results.map { it.id.value })
        }

    @Test
    fun `search matches the description, not only the title`() =
        runTest {
            val results = useCase(BrowsePresetsRequest(query = "ingredients"))

            assertEquals(listOf("food"), results.map { it.id.value })
        }

    @Test
    fun `search matches the category name, so a category can be found by typing it`() =
        runTest {
            val results = useCase(BrowsePresetsRequest(query = "nature"))

            assertEquals(setOf("birds", "zebra"), results.map { it.id.value }.toSet())
        }

    @Test
    fun `search ignores case`() =
        runTest {
            assertEquals(useCase(BrowsePresetsRequest(query = "FOOD")).size, 1)
        }

    /** A learner searching a Polish title cannot be expected to type the diacritics. */
    @Test
    fun `search ignores Polish diacritics on both sides`() =
        runTest {
            val results = useCase(BrowsePresetsRequest(query = "ptaki", languageTag = "pl"))

            assertEquals(listOf("birds"), results.map { it.id.value })
        }

    @Test
    fun `an unmatched query returns nothing rather than everything`() =
        runTest {
            assertTrue(useCase(BrowsePresetsRequest(query = "quantum")).isEmpty())
        }

    @Test
    fun `filtering by category keeps only that category`() =
        runTest {
            val results = useCase(BrowsePresetsRequest(categoryIds = setOf("nature")))

            assertTrue(results.all { it.category.id == "nature" })
            assertEquals(2, results.size)
        }

    @Test
    fun `several categories combine rather than exclude each other`() =
        runTest {
            val results = useCase(BrowsePresetsRequest(categoryIds = setOf("nature", "everyday-life")))

            assertEquals(catalog.size, results.size)
        }

    @Test
    fun `filters and search narrow together`() =
        runTest {
            val results = useCase(BrowsePresetsRequest(query = "zebra", categoryIds = setOf("everyday-life")))

            assertTrue("zebra is in nature, so the category filter must exclude it", results.isEmpty())
        }

    @Test
    fun `the default sort is the editorial popularity ranking`() =
        runTest {
            val results = useCase(BrowsePresetsRequest())

            assertEquals(listOf("greetings", "numbers", "food", "birds", "zebra"), results.map { it.id.value })
        }

    @Test
    fun `alphabetical sort orders by the resolved title`() =
        runTest {
            val results = useCase(BrowsePresetsRequest(sort = PresetSort.ALPHABETICAL))

            assertEquals(listOf("birds", "food", "greetings", "numbers", "zebra"), results.map { it.id.value })
        }

    @Test
    fun `word count sorts run in both directions`() =
        runTest {
            val ascending = useCase(BrowsePresetsRequest(sort = PresetSort.WORD_COUNT_ASCENDING))
            val descending = useCase(BrowsePresetsRequest(sort = PresetSort.WORD_COUNT_DESCENDING))

            assertEquals(listOf(7, 20, 21, 58, 90), ascending.map { it.wordCount })
            assertEquals(listOf(90, 58, 21, 20, 7), descending.map { it.wordCount })
        }

    @Test
    fun `sorting applies to the filtered result, not to the whole catalogue`() =
        runTest {
            val results = useCase(
                BrowsePresetsRequest(categoryIds = setOf("nature"), sort = PresetSort.WORD_COUNT_DESCENDING),
            )

            assertEquals(listOf("birds", "zebra"), results.map { it.id.value })
        }
}
