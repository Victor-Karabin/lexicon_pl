package com.lexicon.presentation.presets

import com.lexicon.interactors.presets.CefrLevel
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetSort
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.VocabularyPreset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

sealed interface PresetBrowserUiState {
    data object Loading : PresetBrowserUiState

    data class Loaded(
        val presets: ImmutableList<VocabularyPreset> = persistentListOf(),
        val categories: ImmutableList<PresetCategory> = persistentListOf(),
        val query: String = "",
        val selectedCategoryIds: Set<String> = emptySet(),
        val selectedCefrLevels: Set<CefrLevel> = emptySet(),
        val sort: PresetSort = PresetSort.POPULARITY,
        val languageTag: String = "en",
        val openedPreset: VocabularyPreset? = null,
        /** Empty while the words are still being resolved, which is why the sheet shows a spinner. */
        val openedPresetWords: ImmutableList<PresetWord> = persistentListOf(),
    ) : PresetBrowserUiState {
        val hasActiveFilters: Boolean
            get() = selectedCategoryIds.isNotEmpty() || selectedCefrLevels.isNotEmpty() || query.isNotBlank()

        /**
         * Separates "nothing matched" from "nothing exists": an empty catalogue is a data
         * problem, an empty result is the user's own filters, and the two need different
         * wording — one offers a way back, the other cannot.
         */
        val isEmptyResult: Boolean get() = presets.isEmpty() && hasActiveFilters

        val isEmptyCatalog: Boolean get() = presets.isEmpty() && !hasActiveFilters
    }
}
