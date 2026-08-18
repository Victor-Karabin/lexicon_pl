package com.lexicon.presentation.conjugation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.conjugation.FavouriteVerbUseCase
import com.lexicon.interactors.conjugation.LoadConjugationVerbsUseCase
import com.lexicon.interactors.conjugation.LoadFavouriteVerbsUseCase
import com.lexicon.interactors.conjugation.LoadSelectedVerbsUseCase
import com.lexicon.interactors.conjugation.SelectConjugationVerbsUseCase
import com.lexicon.interactors.conjugation.VerbConjugation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerbSelectionUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val verbs: ImmutableList<VerbConjugation> = persistentListOf(),
    val selected: ImmutableSet<String> = persistentSetOf(),
    val isSaved: Boolean = false,
    val favourites: ImmutableSet<String> = persistentSetOf(),
) {
    val count: Int get() = selected.size

    val canContinue: Boolean get() = selected.isNotEmpty()
}

class VerbSelectionViewModel(
    private val loadVerbs: LoadConjugationVerbsUseCase,
    private val loadSelected: LoadSelectedVerbsUseCase,
    private val selectVerbs: SelectConjugationVerbsUseCase,
    private val favouriteVerb: FavouriteVerbUseCase,
    private val loadFavourites: LoadFavouriteVerbsUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VerbSelectionUiState())
    val uiState: StateFlow<VerbSelectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            val already = loadSelected()
            val verbs = loadVerbs()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    verbs = verbs,
                    selected = already.toImmutableSet(),
                    favourites = loadFavourites(verbs.map { verb -> verb.infinitive }).toImmutableSet(),
                )
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        viewModelScope.launch(dispatchers.io) {
            val matches = loadVerbs(query)
            val starred = loadFavourites(matches.map { it.infinitive })
            _uiState.update { it.copy(verbs = matches, favourites = starred.toImmutableSet()) }
        }
    }

    fun onVerbToggled(infinitive: String) =
        _uiState.update { state ->
            val next = if (infinitive in state.selected) state.selected - infinitive else state.selected + infinitive
            state.copy(selected = next.toImmutableSet(), isSaved = false)
        }

    fun onFavouriteToggled(verb: VerbConjugation) {
        val next = verb.infinitive !in _uiState.value.favourites
        _uiState.update { state ->
            val favourites = if (next) state.favourites + verb.infinitive else state.favourites - verb.infinitive
            state.copy(favourites = favourites.toImmutableSet())
        }
        viewModelScope.launch(dispatchers.io) {
            favouriteVerb(verb.infinitive, verb.translation, next)
        }
    }

    fun onSave() {
        val chosen = _uiState.value.selected.toList()
        viewModelScope.launch(dispatchers.io) {
            selectVerbs(chosen)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun onSaveHandled() = _uiState.update { it.copy(isSaved = false) }
}
