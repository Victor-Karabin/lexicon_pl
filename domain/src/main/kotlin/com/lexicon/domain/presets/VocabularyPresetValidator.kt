package com.lexicon.domain.presets

import com.lexicon.boundary.VocabularyPresetCatalogBoundary

/** Everything that can be wrong with a preset catalogue, named so a report is actionable. */
sealed interface PresetValidationIssue {
    data class DuplicatePresetId(val id: String) : PresetValidationIssue

    data class DuplicateCategoryId(val id: String) : PresetValidationIssue

    data class UnknownCategory(val presetId: String, val categoryId: String) : PresetValidationIssue

    data class EmptyPreset(val presetId: String) : PresetValidationIssue

    data class DuplicateWordInPreset(val presetId: String, val vocabularyId: Long) : PresetValidationIssue

    data class MissingVocabulary(val presetId: String, val vocabularyIds: List<Long>) : PresetValidationIssue

    data class MissingTitle(val presetId: String) : PresetValidationIssue
}

/**
 * Checks a catalogue against the rules the format promises.
 *
 * The build tool runs the same checks before an asset is written, so shipped data is
 * already valid; this exists for catalogues that did not come from the build tool —
 * downloaded packs, imported files, anything user-supplied later. Reporting every issue
 * rather than throwing on the first lets a bad import be explained in full.
 */
class VocabularyPresetValidator {
    fun validate(
        catalog: VocabularyPresetCatalogBoundary,
        knownVocabularyIds: Set<Long>,
    ): List<PresetValidationIssue> {
        val issues = mutableListOf<PresetValidationIssue>()

        catalog.categories.duplicates { it.id }.forEach { issues += PresetValidationIssue.DuplicateCategoryId(it) }
        catalog.presets.duplicates { it.id }.forEach { issues += PresetValidationIssue.DuplicatePresetId(it) }

        val categoryIds = catalog.categories.map { it.id }.toSet()
        catalog.presets.forEach { preset ->
            if (preset.categoryId !in categoryIds) {
                issues += PresetValidationIssue.UnknownCategory(preset.id, preset.categoryId)
            }
            if (preset.title.values.all { it.isBlank() }) {
                issues += PresetValidationIssue.MissingTitle(preset.id)
            }
            if (preset.vocabularyIds.isEmpty()) {
                issues += PresetValidationIssue.EmptyPreset(preset.id)
            }
            preset.vocabularyIds.duplicates { it }.forEach {
                issues += PresetValidationIssue.DuplicateWordInPreset(preset.id, it)
            }
            // Only checked when the vocabulary is known: an empty set means "not loaded yet",
            // and reporting every word as missing would be noise rather than a finding.
            if (knownVocabularyIds.isNotEmpty()) {
                val missing = preset.vocabularyIds.filterNot { it in knownVocabularyIds }
                if (missing.isNotEmpty()) {
                    issues += PresetValidationIssue.MissingVocabulary(preset.id, missing)
                }
            }
        }
        return issues
    }
}

private fun <T, K> List<T>.duplicates(key: (T) -> K): List<K> = groupingBy(key).eachCount().filterValues { it > 1 }.keys.toList()
