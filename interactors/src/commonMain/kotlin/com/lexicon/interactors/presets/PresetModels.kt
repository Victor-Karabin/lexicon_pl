package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Duration

data class PresetId(val value: String)

data class VocabularyId(val value: Long)

data class LocalizedText(val values: Map<String, String>) {
    companion object {
        const val DEFAULT_LANGUAGE = "en"
    }
}

fun LocalizedText.resolve(languageTag: String): String =
    values[languageTag]
        ?: values[LocalizedText.DEFAULT_LANGUAGE]
        ?: values.values.firstOrNull()
        ?: ""

enum class CefrLevel { A1, A2, B1, B2, C1, C2 }

data class PresetWord(
    val id: VocabularyId,
    val text: String,
    val translation: String,
    val transcription: String,
    val isInStudySet: Boolean = false,
    val cefr: CefrLevel? = null,
)

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
