package com.lexicon.model.vocabulary

import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Duration

data class PresetId(val value: String) {
    companion object {
        val UNSORTED = PresetId("unsorted")
    }
}

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
    val wordCount: Int = vocabularyIds.size,
) {
    val isUnsorted: Boolean get() = id == PresetId.UNSORTED
}
