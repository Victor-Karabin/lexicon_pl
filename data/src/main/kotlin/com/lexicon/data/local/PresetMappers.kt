package com.lexicon.data.local

import com.lexicon.boundary.PresetCategoryBoundary
import com.lexicon.boundary.VocabularyPresetBoundary
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
private val localizedText = MapSerializer(String.serializer(), String.serializer())

internal fun Map<String, String>.encodeLocalized(): String = json.encodeToString(localizedText, this)

internal fun String.decodeLocalized(): Map<String, String> =
    runCatching { json.decodeFromString(localizedText, this) }.getOrDefault(emptyMap())

fun PresetCategoryEntity.toBoundary(): PresetCategoryBoundary =
    PresetCategoryBoundary(id = id, order = sortOrder, title = titleJson.decodeLocalized())

fun PresetEntity.toBoundary(vocabularyIds: List<Long>): VocabularyPresetBoundary =
    VocabularyPresetBoundary(
        id = id,
        categoryId = categoryId,
        title = titleJson.decodeLocalized(),
        description = descriptionJson.decodeLocalized(),
        icon = icon,
        color = color,
        popularity = popularity,
        estimatedSeconds = estimatedSeconds,
        vocabularyIds = vocabularyIds,
    )

fun PresetCategoryAsset.toEntity(): PresetCategoryEntity =
    PresetCategoryEntity(id = id, sortOrder = order, titleJson = title.encodeLocalized())

fun VocabularyPresetAsset.toEntity(): PresetEntity =
    PresetEntity(
        id = id,
        categoryId = category,
        titleJson = title.encodeLocalized(),
        descriptionJson = description.encodeLocalized(),
        icon = icon,
        color = color,
        popularity = popularity,
        estimatedSeconds = estimatedSeconds,
    )

fun VocabularyPresetAsset.toMemberships(): List<PresetWordEntity> =
    vocabularyIds
        .distinct()
        .mapIndexed { index, wordId -> PresetWordEntity(presetId = id, wordId = wordId, position = index) }
