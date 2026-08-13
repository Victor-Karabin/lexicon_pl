package com.lexicon.boundary

data class VocabularyPresetBoundary(
    val id: String,
    val categoryId: String,
    val title: Map<String, String>,
    val description: Map<String, String>,
    val icon: String?,
    val color: String?,
    val popularity: Int,
    val estimatedSeconds: Long,
    val vocabularyIds: List<Long>,
)

data class PresetCategoryBoundary(
    val id: String,
    val order: Int,
    val title: Map<String, String>,
)

data class VocabularyPresetCatalogBoundary(
    val categories: List<PresetCategoryBoundary>,
    val presets: List<VocabularyPresetBoundary>,
)
