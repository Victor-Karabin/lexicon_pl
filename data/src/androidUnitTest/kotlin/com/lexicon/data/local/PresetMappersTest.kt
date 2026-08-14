package com.lexicon.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetMappersTest {
    private val asset = VocabularyPresetAsset(
        id = "food",
        category = "everyday-life",
        title = mapOf("en" to "Food", "pl" to "Jedzenie"),
        description = mapOf("en" to "Meals."),
        icon = "restaurant",
        color = "#EF6C00",
        popularity = 17,
        estimatedSeconds = 3480,
        vocabularyIds = listOf(5L, 3L, 9L),
    )

    @Test
    fun `a preset survives the round trip through storage`() {
        val restored = asset.toEntity().toBoundary(asset.vocabularyIds)

        assertEquals("food", restored.id)
        assertEquals("everyday-life", restored.categoryId)
        assertEquals("Jedzenie", restored.title["pl"])
        assertEquals("Meals.", restored.description["en"])
        assertEquals(3480L, restored.estimatedSeconds)
    }

    @Test
    fun `localized text keeps every language it arrived with`() {
        val encoded = mapOf("en" to "Food", "pl" to "Jedzenie", "de" to "Essen").encodeLocalized()

        assertEquals(3, encoded.decodeLocalized().size)
    }

    @Test
    fun `a column that will not parse degrades to no text rather than throwing`() {
        assertTrue("not json at all".decodeLocalized().isEmpty())
    }

    @Test
    fun `membership records the preset's own order, not the id order`() {
        val memberships = asset.toMemberships()

        assertEquals(listOf(5L, 3L, 9L), memberships.sortedBy { it.position }.map { it.wordId })
    }

    @Test
    fun `a duplicated word in one preset is stored once`() {
        val memberships = asset.copy(vocabularyIds = listOf(1L, 2L, 1L)).toMemberships()

        assertEquals(2, memberships.size)
        assertEquals(memberships.size, memberships.distinctBy { it.wordId }.size)
    }

    @Test
    fun `a category survives the round trip`() {
        val restored = PresetCategoryAsset("nature", 5, mapOf("en" to "Nature")).toEntity().toBoundary()

        assertEquals("nature", restored.id)
        assertEquals(5, restored.order)
        assertEquals("Nature", restored.title["en"])
    }
}
