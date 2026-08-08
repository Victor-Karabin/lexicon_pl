package com.lexicon.domain.presets

import com.lexicon.boundary.PresetCategoryBoundary
import com.lexicon.boundary.VocabularyPresetBoundary
import com.lexicon.boundary.VocabularyPresetCatalogBoundary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyPresetValidatorTest {
    private val validator = VocabularyPresetValidator()

    private fun preset(
        id: String = "food",
        categoryId: String = "everyday-life",
        title: Map<String, String> = mapOf("en" to "Food"),
        vocabularyIds: List<Long> = listOf(1L, 2L, 3L),
    ) = VocabularyPresetBoundary(
        id = id,
        categoryId = categoryId,
        title = title,
        description = mapOf("en" to "Meals."),
        icon = null,
        color = null,
        popularity = 1,
        estimatedSeconds = 180,
        vocabularyIds = vocabularyIds,
    )

    private val category = PresetCategoryBoundary("everyday-life", 3, mapOf("en" to "Everyday life"))

    private fun catalog(vararg presets: VocabularyPresetBoundary) =
        VocabularyPresetCatalogBoundary(categories = listOf(category), presets = presets.toList())

    private val knownWords = setOf(1L, 2L, 3L, 4L)

    @Test
    fun `a well-formed catalogue reports nothing`() {
        assertTrue(validator.validate(catalog(preset()), knownWords).isEmpty())
    }

    @Test
    fun `duplicate preset ids are reported`() {
        val issues = validator.validate(catalog(preset(), preset()), knownWords)

        assertTrue(issues.contains(PresetValidationIssue.DuplicatePresetId("food")))
    }

    @Test
    fun `duplicate category ids are reported`() {
        val duplicated = VocabularyPresetCatalogBoundary(
            categories = listOf(category, category),
            presets = listOf(preset()),
        )

        assertTrue(
            validator.validate(duplicated, knownWords)
                .contains(PresetValidationIssue.DuplicateCategoryId("everyday-life")),
        )
    }

    @Test
    fun `a preset naming an undefined category is reported`() {
        val issues = validator.validate(catalog(preset(categoryId = "invented")), knownWords)

        assertTrue(issues.contains(PresetValidationIssue.UnknownCategory("food", "invented")))
    }

    @Test
    fun `an empty preset is reported`() {
        val issues = validator.validate(catalog(preset(vocabularyIds = emptyList())), knownWords)

        assertTrue(issues.contains(PresetValidationIssue.EmptyPreset("food")))
    }

    @Test
    fun `the same word twice in one preset is reported`() {
        val issues = validator.validate(catalog(preset(vocabularyIds = listOf(1L, 2L, 1L))), knownWords)

        assertTrue(issues.contains(PresetValidationIssue.DuplicateWordInPreset("food", 1L)))
    }

    @Test
    fun `references to words that do not exist are reported`() {
        val issues = validator.validate(catalog(preset(vocabularyIds = listOf(1L, 99L))), knownWords)

        assertTrue(issues.contains(PresetValidationIssue.MissingVocabulary("food", listOf(99L))))
    }

    @Test
    fun `a preset with no usable title is reported`() {
        val issues = validator.validate(catalog(preset(title = mapOf("en" to "  "))), knownWords)

        assertTrue(issues.contains(PresetValidationIssue.MissingTitle("food")))
    }

    /**
     * Callers that have not loaded the vocabulary pass an empty set; treating that as "every
     * word is missing" would bury the real findings under one per word.
     */
    @Test
    fun `an unknown vocabulary is not mistaken for a catalogue full of broken references`() {
        val issues = validator.validate(catalog(preset(vocabularyIds = listOf(1L, 99L))), knownVocabularyIds = emptySet())

        assertTrue(issues.none { it is PresetValidationIssue.MissingVocabulary })
    }

    @Test
    fun `every problem is reported, not just the first`() {
        val broken = preset(id = "broken", categoryId = "invented", vocabularyIds = emptyList())

        val issues = validator.validate(catalog(broken), knownWords)

        assertEquals(2, issues.size)
    }
}
