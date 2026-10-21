package com.lexicon.presentation.countries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.foldAccentsForSearch
import com.lexicon.interactors.countries.Country
import com.lexicon.interactors.countries.LoadCountriesUseCase
import com.lexicon.interactors.countries.LoadCountryChoiceUseCase
import com.lexicon.interactors.countries.SaveCountryChoiceUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CountryCourseUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val countries: ImmutableList<Country> = persistentListOf(),
    val shown: ImmutableList<Country> = persistentListOf(),
    val chosen: ImmutableSet<String> = persistentSetOf(),
    val isSaving: Boolean = false,
) {
    val canStart: Boolean get() = !isSaving && chosen.isNotEmpty()
}

class CountryCourseViewModel(
    private val loadCountries: LoadCountriesUseCase,
    private val loadChoice: LoadCountryChoiceUseCase,
    private val saveChoice: SaveCountryChoiceUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CountryCourseUiState())
    val uiState: StateFlow<CountryCourseUiState> = _uiState.asStateFlow()

    private val _started = MutableSharedFlow<Unit>()
    val started: SharedFlow<Unit> = _started.asSharedFlow()

    private var searchKeys: Map<String, List<String>> = emptyMap()

    init {
        viewModelScope.launch(dispatchers.io) {
            val countries = loadCountries()
            val chosen = loadChoice()
            searchKeys = countries.associate { country ->
                country.code to (country.polish + country.english).map { it.foldAccentsForSearch() }
            }
            _uiState.update {
                it.copy(isLoading = false, countries = countries, shown = countries.matching(it.query), chosen = chosen.toImmutableSet())
            }
        }
    }

    fun onQueryChanged(query: String) = _uiState.update { it.copy(query = query, shown = it.countries.matching(query)) }

    fun onToggled(code: String) =
        _uiState.update {
            it.copy(chosen = (if (code in it.chosen) it.chosen - code else it.chosen + code).toImmutableSet())
        }

    fun onSelectAll() = _uiState.update { it.copy(chosen = (it.chosen + it.shown.map(Country::code)).toImmutableSet()) }

    fun onClear() = _uiState.update { it.copy(chosen = (it.chosen - it.shown.map(Country::code).toSet()).toImmutableSet()) }

    private fun ImmutableList<Country>.matching(query: String): ImmutableList<Country> {
        val needle = query.foldAccentsForSearch()
        return filter { country -> searchKeys.getValue(country.code).any { it.contains(needle) } }.toImmutableList()
    }

    fun onStart() {
        val state = _uiState.value
        if (!state.canStart) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch(dispatchers.io) {
            saveChoice(state.chosen)
            _uiState.update { it.copy(isSaving = false) }
            _started.emit(Unit)
        }
    }
}
