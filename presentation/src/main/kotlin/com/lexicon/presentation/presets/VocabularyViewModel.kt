package com.lexicon.presentation.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.CefrLevel
import com.lexicon.interactors.presets.DeletePresetUseCase
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.ObserveFavouriteWordIdsUseCase
import com.lexicon.interactors.presets.ObserveVocabularyPresetsUseCase
import com.lexicon.interactors.presets.PresetFavouriteState
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.RestorePresetUseCase
import com.lexicon.interactors.presets.RestoreWordUseCase
import com.lexicon.interactors.presets.SearchVocabularyUseCase
import com.lexicon.interactors.presets.SetPresetFavouriteUseCase
import com.lexicon.interactors.presets.ToggleWordFavouriteUseCase
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.VocabularyPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val QUERY_DEBOUNCE_MS = 200L

@HiltViewModel
class VocabularyViewModel
    @Inject
    constructor(
        private val observePresets: ObserveVocabularyPresetsUseCase,
        private val searchVocabulary: SearchVocabularyUseCase,
        private val setPresetFavourite: SetPresetFavouriteUseCase,
        private val toggleWordFavourite: ToggleWordFavouriteUseCase,
        private val deleteWord: DeleteWordUseCase,
        private val restoreWord: RestoreWordUseCase,
        private val deletePreset: DeletePresetUseCase,
        private val restorePreset: RestorePresetUseCase,
        private val observeFavouriteWordIds: ObserveFavouriteWordIdsUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<VocabularyUiState>(VocabularyUiState.Loading)
        val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

        /** Query and levels together, so either changing re-runs the same search. */
        private val criteria = MutableStateFlow(SearchCriteria())

        init {
            // Collected rather than fetched once: a word deleted on the detail screen changes
            // what every preset contains, and a list read at startup would still be showing the
            // old contents when the user comes back to it.
            viewModelScope.launch(dispatchers.io) {
                observePresets().collect { presets ->
                    // Merged rather than assigned: input can arrive before the first emission,
                    // and replacing the state wholesale would silently discard it.
                    _uiState.update { current ->
                        if (current is VocabularyUiState.Loaded) {
                            current.copy(presets = presets)
                        } else {
                            VocabularyUiState.Loaded(presets = presets)
                        }
                    }
                }
            }
            viewModelScope.launch(dispatchers.io) {
                observeFavouriteWordIds().collect { favourites ->
                    updateLoaded { it.copy(favouriteWordIds = favourites) }
                }
            }
            observeQuery()
        }

        /**
         * Debounced so a query is not run per keystroke.
         *
         * Every value is collected, including the initial empty one: dropping the first assumes
         * this collector starts before anything can change the criteria, and a change that
         * arrives first is then dropped instead — the search silently never runs. An empty
         * criteria costs nothing anyway, since the use case answers it without a query.
         */
        @OptIn(FlowPreview::class)
        private fun observeQuery() {
            viewModelScope.launch(dispatchers.io) {
                criteria.debounce(QUERY_DEBOUNCE_MS).collect { current ->
                    searchJob?.cancel()
                    searchJob = viewModelScope.launch(dispatchers.io) {
                        val results = searchVocabulary(current.query, current.levels)
                        updateLoaded { it.copy(words = results, isSearching = false) }
                    }
                }
            }
        }

        private var searchJob: Job? = null

        fun onQueryChanged(value: String) {
            criteria.update { it.copy(query = value) }
            // Applied immediately rather than after the debounce, so the list switches to
            // words on the first keystroke instead of lagging a fifth of a second behind.
            updateLoaded { it.copy(query = value).clearedWordsIfIdle() }
        }

        /**
         * A level lists the words at it rather than narrowing the presets, so this drives the
         * search instead of the preset query.
         */
        fun onCefrToggled(level: CefrLevel) {
            val updated = (_uiState.value as? VocabularyUiState.Loaded)
                ?.selectedCefrLevels?.toggle(level) ?: return
            criteria.update { it.copy(levels = updated) }
            updateLoaded { it.copy(selectedCefrLevels = updated).clearedWordsIfIdle() }
        }

        fun onFiltersCleared() {
            criteria.update { it.copy(levels = emptySet()) }
            updateLoaded { it.copy(selectedCefrLevels = emptySet()).clearedWordsIfIdle() }
        }

        /** Partly-favourited counts as off, so one tap completes the preset rather than clearing it. */
        fun onPresetFavouriteToggled(
            id: PresetId,
            current: PresetFavouriteState,
        ) {
            viewModelScope.launch(dispatchers.io) {
                setPresetFavourite(id, current != PresetFavouriteState.ALL)
            }
        }

        fun onWordFavouriteToggled(
            id: VocabularyId,
            isFavourite: Boolean,
        ) {
            viewModelScope.launch(dispatchers.io) { toggleWordFavourite(id, isFavourite) }
        }

        /** Deletes at once, as the button says, and keeps enough to put it back. */
        fun onWordDeleted(word: PresetWord) {
            viewModelScope.launch(dispatchers.io) {
                deleteWord(word.id)
                updateLoaded {
                    it.copy(
                        words = it.words.filterNot { candidate -> candidate.id == word.id }.toImmutableList(),
                        lastDeleted = DeletedItem.Word(word.id, word.text),
                    )
                }
            }
        }

        fun onPresetDeleted(preset: VocabularyPreset) {
            viewModelScope.launch(dispatchers.io) {
                deletePreset(preset.id)
                updateLoaded {
                    it.copy(
                        presets = it.presets.filterNot { candidate -> candidate.id == preset.id }.toImmutableList(),
                        lastDeleted = DeletedItem.Preset(preset.id, preset.title.resolve(it.languageTag)),
                    )
                }
            }
        }

        fun onUndoDelete() {
            val deleted = (_uiState.value as? VocabularyUiState.Loaded)?.lastDeleted ?: return
            viewModelScope.launch(dispatchers.io) {
                when (deleted) {
                    is DeletedItem.Word -> {
                        restoreWord(deleted.id)
                        refreshWords()
                    }

                    // No refresh: restoring re-imports the catalogue, and the observed list
                    // reports it.
                    is DeletedItem.Preset -> restorePreset(deleted.id)
                }
                updateLoaded { it.copy(lastDeleted = null) }
            }
        }

        /** Cleared once shown, so the same message cannot be offered twice. */
        fun onDeleteMessageShown() = updateLoaded { it.copy(lastDeleted = null) }

        private suspend fun refreshWords() {
            val current = criteria.value
            val results = searchVocabulary(current.query, current.levels)
            updateLoaded { it.copy(words = results) }
        }

        private fun updateLoaded(transform: (VocabularyUiState.Loaded) -> VocabularyUiState.Loaded) {
            _uiState.update { if (it is VocabularyUiState.Loaded) transform(it) else it }
        }
    }

private data class SearchCriteria(
    val query: String = "",
    val levels: Set<CefrLevel> = emptySet(),
)

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

/** Drops stale results the moment nothing is being asked for, so the presets are not covered. */
private fun VocabularyUiState.Loaded.clearedWordsIfIdle(): VocabularyUiState.Loaded =
    if (isSearchingWords) copy(isSearching = true) else copy(isSearching = false, words = persistentListOf())
