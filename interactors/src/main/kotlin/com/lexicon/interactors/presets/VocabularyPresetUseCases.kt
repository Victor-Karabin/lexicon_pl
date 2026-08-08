package com.lexicon.interactors.presets

import kotlinx.collections.immutable.ImmutableList

/** Every preset, ordered by category and then by the catalogue's own order. */
interface GetVocabularyPresetsUseCase {
    suspend operator fun invoke(): ImmutableList<VocabularyPreset>
}

interface GetPresetCategoriesUseCase {
    suspend operator fun invoke(): ImmutableList<PresetCategory>
}

/** Null when nothing carries the id, which happens if saved state outlives a preset. */
interface GetVocabularyPresetUseCase {
    suspend operator fun invoke(id: PresetId): VocabularyPreset?
}

/** The words a preset contains, for the trainings that are about to use them. */
interface GetPresetVocabularyUseCase {
    suspend operator fun invoke(id: PresetId): ImmutableList<PresetWord>
}

enum class PresetSort {
    /** The catalogue's editorial ranking. */
    POPULARITY,
    ALPHABETICAL,
    WORD_COUNT_ASCENDING,
    WORD_COUNT_DESCENDING,
}

data class BrowsePresetsRequest(
    val query: String = "",
    val categoryIds: Set<String> = emptySet(),
    val sort: PresetSort = PresetSort.POPULARITY,
    /** Resolves localized titles for both searching and alphabetical ordering. */
    val languageTag: String = LocalizedText.DEFAULT_LANGUAGE,
)

/** Searching, filtering and sorting in one call, so the rules live in one place. */
interface BrowseVocabularyPresetsUseCase {
    suspend operator fun invoke(request: BrowsePresetsRequest): ImmutableList<VocabularyPreset>
}
