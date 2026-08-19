package com.lexicon.presentation.presets

import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.VocabularyPreset
import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

sealed interface DeletedItem {
    val label: String

    data class Word(val id: VocabularyId, override val label: String) : DeletedItem

    data class Words(val ids: List<VocabularyId>, override val label: String = "") : DeletedItem

    data class Preset(val id: PresetId, override val label: String) : DeletedItem
}

sealed interface VocabularyUiState {
    data object Loading : VocabularyUiState

    data class Loaded(
        val query: String = "",
        val presets: ImmutableList<VocabularyPreset> = persistentListOf(),
        val selectedCefrLevels: Set<CefrLevel> = emptySet(),
        val words: ImmutableList<Word> = persistentListOf(),
        val isSearching: Boolean = false,
        val languageTag: String = "en",
        val studySetWordIds: Set<VocabularyId> = emptySet(),
        val lastDeleted: DeletedItem? = null,
    ) : VocabularyUiState
}

val VocabularyUiState.Loaded.isSearchingWords: Boolean
    get() = query.isNotBlank() || selectedCefrLevels.isNotEmpty()

val VocabularyUiState.Loaded.hasNoMatchingWords: Boolean
    get() = isSearchingWords && !isSearching && words.isEmpty()

val VocabularyUiState.Loaded.hasNoPresetsAtAll: Boolean
    get() = !isSearchingWords && presets.isEmpty()
