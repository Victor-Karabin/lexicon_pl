package com.lexicon.boundary

/**
 * A preset as the data layer knows it: identifiers and plain values only.
 *
 * Vocabulary is carried as ids rather than whole items so that a preset stays a small,
 * cheap object. The thousand-word presets would otherwise duplicate most of the corpus.
 */
data class VocabularyPresetBoundary(
    val id: String,
    val categoryId: String,
    val title: Map<String, String>,
    val description: Map<String, String>,
    val icon: String?,
    val color: String?,
    val cefr: String?,
    val popularity: Int,
    val estimatedSeconds: Long,
    val vocabularyIds: List<Long>,
)

data class PresetCategoryBoundary(
    val id: String,
    val order: Int,
    val title: Map<String, String>,
)

/** Presets and the categories they are grouped under, as read from one source. */
data class VocabularyPresetCatalogBoundary(
    val categories: List<PresetCategoryBoundary>,
    val presets: List<VocabularyPresetBoundary>,
)
