package com.lexicon.data.local

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VocabularyPresetAssetTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a catalogue parses into boundary types`() {
        val raw = """
            {
              "categories": [{"id": "everyday-life", "order": 3, "title": {"en": "Everyday life"}}],
              "presets": [{
                "id": "food", "category": "everyday-life",
                "title": {"en": "Food"}, "description": {"en": "Meals."},
                "icon": "restaurant", "color": "#EF6C00", "cefr": "A1",
                "popularity": 17, "estimatedSeconds": 3480, "vocabularyIds": [1, 2, 3]
              }]
            }
        """.trimIndent()

        val catalog = json.decodeFromString<VocabularyPresetCatalogAsset>(raw).toBoundary()

        val preset = catalog.presets.single()
        assertEquals("food", preset.id)
        assertEquals("everyday-life", preset.categoryId)
        assertEquals("Food", preset.title["en"])
        assertEquals("A1", preset.cefr)
        assertEquals(3480L, preset.estimatedSeconds)
        assertEquals(listOf(1L, 2L, 3L), preset.vocabularyIds)
        assertEquals(3, catalog.categories.single().order)
    }

    /** A pack built by an older app version must keep loading after the format grows. */
    @Test
    fun `a preset missing every optional field still parses`() {
        val raw = """{"presets": [{"id": "minimal", "category": "everyday-life"}]}"""

        val preset = json.decodeFromString<VocabularyPresetCatalogAsset>(raw).toBoundary().presets.single()

        assertNull(preset.icon)
        assertNull(preset.cefr)
        assertTrue(preset.vocabularyIds.isEmpty())
    }

    /** And a pack built by a *newer* one must not fail on fields this version has never seen. */
    @Test
    fun `unknown fields are ignored rather than rejected`() {
        val raw = """
            {"presets": [{"id": "future", "category": "everyday-life", "premium": true, "packUrl": "x"}]}
        """.trimIndent()

        assertNotNull(json.decodeFromString<VocabularyPresetCatalogAsset>(raw))
    }

    /**
     * Guards the shipped asset itself, not just the parser: the build tool validates before
     * writing, but nothing stops the file being hand-edited afterwards.
     */
    @Test
    fun `the bundled catalogue parses and every preset is usable`() {
        val file = File("src/main/assets/vocabulary_presets.json")
        assertTrue("expected the built asset at ${file.absolutePath}", file.exists())

        val catalog = json.decodeFromString<VocabularyPresetCatalogAsset>(file.readText()).toBoundary()
        val categoryIds = catalog.categories.map { it.id }.toSet()
        val vocabularyIds = json
            .decodeFromString<List<VocabularySeedItem>>(File("src/main/assets/vocabulary_pl.json").readText())
            .map { it.id }
            .toSet()

        assertTrue("the catalogue is empty", catalog.presets.isNotEmpty())
        assertEquals(
            "preset ids must be unique",
            catalog.presets.size,
            catalog.presets.map { it.id }.distinct().size,
        )
        catalog.presets.forEach { preset ->
            assertTrue("${preset.id} names an unknown category", preset.categoryId in categoryIds)
            assertTrue("${preset.id} is empty", preset.vocabularyIds.isNotEmpty())
            assertTrue("${preset.id} has no English title", !preset.title["en"].isNullOrBlank())
            assertTrue(
                "${preset.id} references words that are not in the vocabulary",
                vocabularyIds.containsAll(preset.vocabularyIds),
            )
            assertEquals(
                "${preset.id} lists the same word twice",
                preset.vocabularyIds.size,
                preset.vocabularyIds.distinct().size,
            )
        }
    }
}
