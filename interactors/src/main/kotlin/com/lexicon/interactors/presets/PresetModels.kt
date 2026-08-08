package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Duration

/** Stable, human-readable preset key: it appears in data files and in saved user state. */
@JvmInline
value class PresetId(val value: String)

@JvmInline
value class VocabularyId(val value: Long)

/**
 * Text carried in every language the data file supplies, resolved at the edge rather than
 * at load time — the display language can change without reloading the catalogue.
 */
data class LocalizedText(private val values: Map<String, String>) {
    /** Falls back to English, then to any available translation, so nothing renders blank. */
    fun resolve(languageTag: String): String = values[languageTag] ?: values[DEFAULT_LANGUAGE] ?: values.values.firstOrNull() ?: ""

    companion object {
        const val DEFAULT_LANGUAGE = "en"
    }
}

enum class CefrLevel { A1, A2, B1, B2, C1, C2 }

/** A vocabulary item as delivered by a preset, for whatever is about to train on it. */
data class PresetWord(
    val id: VocabularyId,
    val text: String,
    val translation: String,
    val transcription: String,
    /** Favourited words are the ones trainings draw from; see [ToggleWordFavouriteUseCase]. */
    val isFavourite: Boolean = false,
)

data class PresetCategory(
    val id: String,
    val order: Int,
    val title: LocalizedText,
)

/**
 * A curated collection of vocabulary. Holds ids rather than items so a preset can be
 * listed, searched and sorted without touching the vocabulary store at all; the words are
 * fetched only when a session actually starts.
 */
data class VocabularyPreset(
    val id: PresetId,
    val title: LocalizedText,
    val description: LocalizedText,
    val category: PresetCategory,
    val cefr: CefrLevel?,
    val icon: String?,
    val color: String?,
    val popularity: Int,
    val estimatedDuration: Duration,
    val vocabularyIds: ImmutableList<VocabularyId>,
) {
    val wordCount: Int get() = vocabularyIds.size
}
