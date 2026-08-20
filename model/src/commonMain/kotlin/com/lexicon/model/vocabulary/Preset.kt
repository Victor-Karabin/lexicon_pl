package com.lexicon.model.vocabulary

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
    val wordCount: Int = vocabularyIds.size,
    val studySetCount: Int = 0,
) {
    val studySetState: PresetStudySetState get() = PresetStudySetState.of(wordCount, studySetCount)
}

enum class PresetStudySetState {
    NONE,
    SOME,
    ALL,
    ;

    companion object {
        fun of(
            wordCount: Int,
            studySetCount: Int,
        ): PresetStudySetState =
            when {
                studySetCount <= 0 -> NONE
                studySetCount >= wordCount -> ALL
                else -> SOME
            }
    }
}
