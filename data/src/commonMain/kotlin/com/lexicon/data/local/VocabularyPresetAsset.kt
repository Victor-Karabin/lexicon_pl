package com.lexicon.data.local

import com.lexicon.boundary.PresetCategoryBoundary
import com.lexicon.boundary.VocabularyPresetBoundary
import com.lexicon.boundary.VocabularyPresetCatalogBoundary
import kotlinx.serialization.Serializable

@Serializable
data class VocabularyPresetCatalogAsset(
    val categories: List<PresetCategoryAsset> = emptyList(),
    val presets: List<VocabularyPresetAsset> = emptyList(),
)

@Serializable
data class PresetCategoryAsset(
    val id: String,
    val order: Int = 0,
    val title: Map<String, String> = emptyMap(),
)

@Serializable
data class VocabularyPresetAsset(
    val id: String,
    val category: String,
    val title: Map<String, String> = emptyMap(),
    val description: Map<String, String> = emptyMap(),
    val icon: String? = null,
    val color: String? = null,
    val popularity: Int = Int.MAX_VALUE,
    val estimatedSeconds: Long = 0,
    val vocabularyIds: List<Long> = emptyList(),
)

fun VocabularyPresetCatalogAsset.toBoundary(): VocabularyPresetCatalogBoundary =
    VocabularyPresetCatalogBoundary(
        categories = categories.map {
            PresetCategoryBoundary(id = it.id, order = it.order, title = it.title)
        },
        presets = presets.map {
            VocabularyPresetBoundary(
                id = it.id,
                categoryId = it.category,
                title = it.title,
                description = it.description,
                icon = it.icon,
                color = it.color,
                popularity = it.popularity,
                estimatedSeconds = it.estimatedSeconds,
                vocabularyIds = it.vocabularyIds,
                wordCount = it.vocabularyIds.size,
                studySetCount = 0,
            )
        },
    )
