package com.lexicon.presentation.presets

import com.lexicon.interactors.presets.CefrLevel
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

sealed interface DeletedItem {
    val label: String

    data class Word(val id: VocabularyId, override val label: String) : DeletedItem

    /** Several at once, from the selection bar. [label] is unused; the count is. */
    data class Words(val ids: List<VocabularyId>, override val label: String = "") : DeletedItem

    data class Preset(val id: PresetId, override val label: String) : DeletedItem
}

sealed interface VocabularyUiState {
    data object Loading : VocabularyUiState

    data class Loaded(
        val query: String = "",
        val presets: ImmutableList<VocabularyPreset> = persistentListOf(),
        val selectedCefrLevels: Set<CefrLevel> = emptySet(),
        val words: ImmutableList<PresetWord> = persistentListOf(),
        val isSearching: Boolean = false,
        val languageTag: String = "en",
        val favouriteWordIds: Set<VocabularyId> = emptySet(),
        val lastDeleted: DeletedItem? = null,
    ) : VocabularyUiState
}

val VocabularyUiState.Loaded.isSearchingWords: Boolean
    get() = query.isNotBlank() || selectedCefrLevels.isNotEmpty()

val VocabularyUiState.Loaded.hasNoMatchingWords: Boolean
    get() = isSearchingWords && !isSearching && words.isEmpty()

val VocabularyUiState.Loaded.hasNoPresetsAtAll: Boolean
    get() = !isSearchingWords && presets.isEmpty()
