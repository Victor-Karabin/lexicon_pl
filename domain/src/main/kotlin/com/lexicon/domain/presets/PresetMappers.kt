package com.lexicon.domain.presets

import com.lexicon.boundary.PresetCategoryBoundary
import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyPresetBoundary
import com.lexicon.interactors.presets.CefrLevel
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Duration.Companion.seconds

fun PresetCategoryBoundary.toCategory(): PresetCategory = PresetCategory(id = id, order = order, title = LocalizedText(title))

/**
 * An unrecognised CEFR value degrades to null rather than throwing: a data file written
 * against a newer scale should cost a filter, not the whole catalogue.
 */
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

fun VocabularyItemBoundary.toPresetWord(): PresetWord =
    PresetWord(
        id = VocabularyId(id),
        text = text,
        translation = translation,
        transcription = transcription,
        isFavourite = isFavourite,
        cefr = cefr?.let { value -> CefrLevel.entries.firstOrNull { it.name == value } },
    )
