package com.lexicon.domain.presets

import com.lexicon.boundary.PresetCategoryBoundary
import com.lexicon.boundary.VocabularyPresetBoundary
import com.lexicon.model.vocabulary.LocalizedText
import com.lexicon.model.vocabulary.PresetCategory
import com.lexicon.model.vocabulary.PresetId
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.VocabularyPreset
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Duration.Companion.seconds

fun PresetCategoryBoundary.toCategory(): PresetCategory = PresetCategory(id = id, order = order, title = LocalizedText(title))

fun VocabularyPresetBoundary.toPreset(category: PresetCategory): VocabularyPreset =
    VocabularyPreset(
        id = PresetId(id),
        title = LocalizedText(title),
        description = LocalizedText(description),
        category = category,
        icon = icon,
        color = color,
        popularity = popularity,
        estimatedDuration = estimatedSeconds.seconds,
        vocabularyIds = vocabularyIds.map(::VocabularyId).toImmutableList(),
    )
