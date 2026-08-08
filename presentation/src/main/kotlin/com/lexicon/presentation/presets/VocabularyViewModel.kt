package com.lexicon.presentation.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.BrowsePresetsRequest
import com.lexicon.interactors.presets.BrowseVocabularyPresetsUseCase
import com.lexicon.interactors.presets.CefrLevel
import com.lexicon.interactors.presets.GetPresetCategoriesUseCase
import com.lexicon.interactors.presets.ObserveFavouriteWordIdsUseCase
import com.lexicon.interactors.presets.PresetFavouriteState
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.SearchVocabularyUseCase
import com.lexicon.interactors.presets.SetPresetFavouriteUseCase
import com.lexicon.interactors.presets.ToggleWordFavouriteUseCase
import com.lexicon.interactors.presets.VocabularyId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val QUERY_DEBOUNCE_MS = 200L

@HiltViewModel
class VocabularyViewModel
    @Inject
    constructor(
        private val browsePresets: BrowseVocabularyPresetsUseCase,
        private val getCategories: GetPresetCategoriesUseCase,
        private val searchVocabulary: SearchVocabularyUseCase,
        private val setPresetFavourite: SetPresetFavouriteUseCase,
        private val toggleWordFavourite: ToggleWordFavouriteUseCase,
        private val observeFavouriteWordIds: ObserveFavouriteWordIdsUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<VocabularyUiState>(VocabularyUiState.Loading)
        val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

        /** Query and levels together, so either changing re-runs the same search. */
        private val criteria = MutableStateFlow(SearchCriteria())

        init {
            viewModelScope.launch(dispatchers.io) {
                _uiState.value = VocabularyUiState.Loaded(categories = getCategories())
                refreshPresets()
            }
            viewModelScope.launch(dispatchers.io) {
                observeFavouriteWordIds().collect { favourites ->
                    updateLoaded { it.copy(favouriteWordIds = favourites) }
                }
            }
            observeQuery()
        }

        /**
         * Debounced so a query is not run per keystroke. `drop(1)` skips the initial empty
         * value, which would otherwise run a search before anything is typed.
         */
        @OptIn(FlowPreview::class)
        private fun observeQuery() {
            viewModelScope.launch(dispatchers.io) {
                criteria.drop(1).debounce(QUERY_DEBOUNCE_MS).collect { current ->
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

        /** Filters toggle rather than replace, so several categories can be combined. */
        fun onCategoryToggled(categoryId: String) =
            updateAndRefresh { it.copy(selectedCategoryIds = it.selectedCategoryIds.toggle(categoryId)) }

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
            updateAndRefresh { it.copy(selectedCategoryIds = emptySet(), selectedCefrLevels = emptySet()) }
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

        private fun updateAndRefresh(transform: (VocabularyUiState.Loaded) -> VocabularyUiState.Loaded) {
            updateLoaded(transform)
            viewModelScope.launch(dispatchers.io) { refreshPresets() }
        }

        /**
         * Re-runs the whole preset query on every change rather than narrowing the visible
         * list: clearing a filter has to *widen* the results, which an already-narrowed list
         * cannot do.
         */
        private suspend fun refreshPresets() {
            val state = _uiState.value as? VocabularyUiState.Loaded ?: return
            val results = browsePresets(
                BrowsePresetsRequest(
                    categoryIds = state.selectedCategoryIds,
                    languageTag = state.languageTag,
                ),
            )
            updateLoaded { it.copy(presets = results) }
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
