package com.lexicon.presentation.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.BrowsePresetsRequest
import com.lexicon.interactors.presets.BrowseVocabularyPresetsUseCase
import com.lexicon.interactors.presets.CefrLevel
import com.lexicon.interactors.presets.GetPresetCategoriesUseCase
import com.lexicon.interactors.presets.GetPresetVocabularyUseCase
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetSort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PresetBrowserViewModel
    @Inject
    constructor(
        private val browsePresets: BrowseVocabularyPresetsUseCase,
        private val getCategories: GetPresetCategoriesUseCase,
        private val getPresetVocabulary: GetPresetVocabularyUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PresetBrowserUiState>(PresetBrowserUiState.Loading)
        val uiState: StateFlow<PresetBrowserUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch(dispatchers.io) {
                _uiState.value = PresetBrowserUiState.Loaded(categories = getCategories())
                refresh()
            }
        }

        fun onQueryChanged(query: String) = updateAndRefresh { it.copy(query = query) }

        fun onSortSelected(sort: PresetSort) = updateAndRefresh { it.copy(sort = sort) }

        /** Filters toggle rather than replace, so several categories can be combined. */
        fun onCategoryToggled(categoryId: String) =
            updateAndRefresh { it.copy(selectedCategoryIds = it.selectedCategoryIds.toggle(categoryId)) }

        fun onCefrToggled(level: CefrLevel) = updateAndRefresh { it.copy(selectedCefrLevels = it.selectedCefrLevels.toggle(level)) }

        /**
         * Shows the preset immediately and fills in its words when they arrive: resolving a
         * thousand ids takes a query, and an empty sheet that appears at once reads better
         * than a tap that seems to do nothing.
         */
        fun onPresetOpened(id: PresetId) {
            val preset = (_uiState.value as? PresetBrowserUiState.Loaded)
                ?.presets?.firstOrNull { it.id == id } ?: return
            _uiState.update {
                if (it is PresetBrowserUiState.Loaded) {
                    it.copy(openedPreset = preset, openedPresetWords = persistentListOf())
                } else {
                    it
                }
            }
            viewModelScope.launch(dispatchers.io) {
                val words = getPresetVocabulary(id)
                _uiState.update { state ->
                    // The sheet may have been dismissed, or another preset opened, while
                    // this was loading; only apply the words if they still belong on screen.
                    if (state is PresetBrowserUiState.Loaded && state.openedPreset?.id == id) {
                        state.copy(openedPresetWords = words)
                    } else {
                        state
                    }
                }
            }
        }

        fun onPresetClosed() =
            _uiState.update {
                if (it is PresetBrowserUiState.Loaded) {
                    it.copy(openedPreset = null, openedPresetWords = persistentListOf())
                } else {
                    it
                }
            }

        fun onFiltersCleared() =
            updateAndRefresh {
                it.copy(query = "", selectedCategoryIds = emptySet(), selectedCefrLevels = emptySet())
            }

        private fun updateAndRefresh(transform: (PresetBrowserUiState.Loaded) -> PresetBrowserUiState.Loaded) {
            _uiState.update { if (it is PresetBrowserUiState.Loaded) transform(it) else it }
            viewModelScope.launch(dispatchers.io) { refresh() }
        }

        /**
         * Re-runs the whole query on every change rather than narrowing the visible list:
         * clearing a filter has to *widen* the results, which an already-narrowed list cannot do.
         */
        private suspend fun refresh() {
            val state = _uiState.value as? PresetBrowserUiState.Loaded ?: return
            val results = browsePresets(
                BrowsePresetsRequest(
                    query = state.query,
                    categoryIds = state.selectedCategoryIds,
                    cefrLevels = state.selectedCefrLevels,
                    sort = state.sort,
                    languageTag = state.languageTag,
                ),
            )
            _uiState.update { if (it is PresetBrowserUiState.Loaded) it.copy(presets = results) else it }
        }
    }

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
