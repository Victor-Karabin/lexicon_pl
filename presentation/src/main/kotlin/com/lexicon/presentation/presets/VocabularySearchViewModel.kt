package com.lexicon.presentation.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.ObserveFavouriteWordIdsUseCase
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.SearchVocabularyUseCase
import com.lexicon.interactors.presets.ToggleWordFavouriteUseCase
import com.lexicon.interactors.presets.VocabularyId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val QUERY_DEBOUNCE_MS = 200L

data class VocabularySearchUiState(
    val query: String = "",
    val results: ImmutableList<PresetWord> = persistentListOf(),
    val isSearching: Boolean = false,
) {
    val hasQuery: Boolean get() = query.isNotBlank()

    /** Only "no matches" once a search has actually run, or it flashes while you type. */
    val isEmptyResult: Boolean get() = hasQuery && !isSearching && results.isEmpty()
}

@HiltViewModel
class VocabularySearchViewModel
    @Inject
    constructor(
        private val searchVocabulary: SearchVocabularyUseCase,
        private val toggleWordFavourite: ToggleWordFavouriteUseCase,
        observeFavouriteWordIds: ObserveFavouriteWordIdsUseCase,
        private val dispatchers: DispatcherProvider,
    ) : ViewModel() {
        private val query = MutableStateFlow("")
        private val searching = MutableStateFlow(false)

        @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
        private val results =
            query
                // Debounced so a query is not run per keystroke, and mapLatest so a slow one
                // cannot land after the query that replaced it.
                .debounce(QUERY_DEBOUNCE_MS)
                .mapLatest { typed ->
                    searching.value = true
                    searchVocabulary(typed).also { searching.value = false }
                }

        val uiState: StateFlow<VocabularySearchUiState> =
            combine(query, results, searching, observeFavouriteWordIds()) { typed, found, isSearching, favourites ->
                VocabularySearchUiState(
                    query = typed,
                    // Re-derived from the live set rather than from the row the search returned,
                    // so a heart set elsewhere is already right when results come back.
                    results = found.map { it.copy(isFavourite = it.id in favourites) }.toImmutableList(),
                    isSearching = isSearching,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = VocabularySearchUiState(),
            )

        fun onQueryChanged(value: String) {
            query.value = value
        }

        fun onFavouriteToggled(
            id: VocabularyId,
            isFavourite: Boolean,
        ) {
            viewModelScope.launch(dispatchers.io) { toggleWordFavourite(id, isFavourite) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
