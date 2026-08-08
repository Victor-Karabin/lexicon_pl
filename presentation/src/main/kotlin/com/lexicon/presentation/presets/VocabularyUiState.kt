package com.lexicon.presentation.presets

import com.lexicon.interactors.presets.CefrLevel
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** What was deleted, and what to say about it. */
sealed interface DeletedItem {
    val label: String

    data class Word(val id: VocabularyId, override val label: String) : DeletedItem

    data class Preset(val id: PresetId, override val label: String) : DeletedItem
}

sealed interface VocabularyUiState {
    data object Loading : VocabularyUiState

    /**
     * One screen with two things on it: the presets, and — while a query is typed or a level
     * picked — the words matching. The presets are kept rather than cleared, so backing out of
     * a search puts the list back exactly as it was.
     */
    data class Loaded(
        val query: String = "",
        val presets: ImmutableList<VocabularyPreset> = persistentListOf(),
        val selectedCefrLevels: Set<CefrLevel> = emptySet(),
        val words: ImmutableList<PresetWord> = persistentListOf(),
        val isSearching: Boolean = false,
        val languageTag: String = "en",
        /**
         * The whole set rather than a flag per preset: presets overlap heavily, so deriving
         * each card's state from one set is both cheaper and impossible to get out of step.
         */
        val favouriteWordIds: Set<VocabularyId> = emptySet(),
        /**
         * The last deletion, kept so it can be undone. A swipe is easy to make by accident and
         * the item is otherwise gone for good — the deletion outlives a catalogue sync.
         */
        val lastDeleted: DeletedItem? = null,
    ) : VocabularyUiState {
        /**
         * Words are listed when something is typed or a level is picked; with neither, the
         * screen is a preset browser.
         */
        val isSearchingWords: Boolean get() = query.isNotBlank() || selectedCefrLevels.isNotEmpty()

        /** Only "no matches" once a search has settled, or it flashes between keystrokes. */
        val hasNoMatchingWords: Boolean get() = isSearchingWords && !isSearching && words.isEmpty()

        /** The catalogue is fixed, so an empty preset list can only mean missing data. */
        val hasNoPresetsAtAll: Boolean get() = !isSearchingWords && presets.isEmpty()
    }
}
