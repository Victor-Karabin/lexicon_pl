package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.LocalizedText
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Duration

data class PresetId(val value: String)

data class PresetCategory(
    val id: String,
    val order: Int,
    val title: LocalizedText,
)

data class VocabularyPreset(
    val id: PresetId,
    val title: LocalizedText,
    val description: LocalizedText,
    val category: PresetCategory,
    val icon: String?,
    val color: String?,
    val popularity: Int,
    val estimatedDuration: Duration,
    val vocabularyIds: ImmutableList<VocabularyId>,
)

val VocabularyPreset.wordCount: Int get() = vocabularyIds.size
