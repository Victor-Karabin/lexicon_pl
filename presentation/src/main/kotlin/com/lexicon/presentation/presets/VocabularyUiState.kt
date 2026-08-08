package com.lexicon.presentation.presets

import com.lexicon.interactors.presets.CefrLevel
import com.lexicon.interactors.presets.PresetCategory
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

sealed interface VocabularyUiState {
    data object Loading : VocabularyUiState

    /**
     * One screen with two things on it: the presets, and — while something is typed — the
     * words matching it. The presets are kept rather than cleared, so backing out of a search
     * puts the list back exactly as it was, filters and sort included.
     */
    data class Loaded(
        val query: String = "",
        val presets: ImmutableList<VocabularyPreset> = persistentListOf(),
        val categories: ImmutableList<PresetCategory> = persistentListOf(),
        val selectedCategoryIds: Set<String> = emptySet(),
        val selectedCefrLevels: Set<CefrLevel> = emptySet(),
        val words: ImmutableList<PresetWord> = persistentListOf(),
        val isSearching: Boolean = false,
        val languageTag: String = "en",
        /**
         * The whole set rather than a flag per preset: presets overlap heavily, so deriving
         * each card's state from one set is both cheaper and impossible to get out of step.
         */
        val favouriteWordIds: Set<VocabularyId> = emptySet(),
    ) : VocabularyUiState {
        /**
         * Words are listed when something is typed or a level is picked; with neither, the
         * screen is a preset browser.
         */
        val isSearchingWords: Boolean get() = query.isNotBlank() || selectedCefrLevels.isNotEmpty()

        /** Only categories narrow the presets; levels belong to the words. */
        val hasActiveFilters: Boolean get() = selectedCategoryIds.isNotEmpty()

        /** Only "no matches" once a search has settled, or it flashes between keystrokes. */
        val hasNoMatchingWords: Boolean get() = isSearchingWords && !isSearching && words.isEmpty()

        /**
         * Separates "nothing matched" from "nothing exists": an empty catalogue is a data
         * problem, an empty result is the user's own filters, and the two need different wording.
         */
        val hasNoMatchingPresets: Boolean get() = !isSearchingWords && presets.isEmpty() && hasActiveFilters

        val hasNoPresetsAtAll: Boolean get() = !isSearchingWords && presets.isEmpty() && !hasActiveFilters
    }
}
