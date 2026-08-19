package com.lexicon.presentation.conjugation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.conjugation.CreateConjugationCourseUseCase
import com.lexicon.interactors.conjugation.DeleteConjugationVerbUseCase
import com.lexicon.interactors.conjugation.HasDeletedVerbsUseCase
import com.lexicon.interactors.conjugation.LoadConjugationVerbsUseCase
import com.lexicon.interactors.conjugation.LoadStudySetVerbsUseCase
import com.lexicon.interactors.conjugation.RestoreConjugationVerbsUseCase
import com.lexicon.interactors.conjugation.ToggleVerbInStudySetUseCase
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
    val canRestore: Boolean = false,
    val studySet: ImmutableSet<String> = persistentSetOf(),
) {
    val count: Int get() = selected.size

    val canContinue: Boolean get() = selected.isNotEmpty()
}

class VerbSelectionViewModel(
    private val loadVerbs: LoadConjugationVerbsUseCase,
    private val createCourse: CreateConjugationCourseUseCase,
    private val deleteVerb: DeleteConjugationVerbUseCase,
    private val restoreVerbs: RestoreConjugationVerbsUseCase,
    private val hasDeletedVerbs: HasDeletedVerbsUseCase,
    private val studySetVerb: ToggleVerbInStudySetUseCase,
    private val loadStudySet: LoadStudySetVerbsUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VerbSelectionUiState())
    val uiState: StateFlow<VerbSelectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            // Nothing starts ticked: this screen builds a new course each time it opens.
            val verbs = loadVerbs()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    verbs = verbs,
                    studySet = loadStudySet(verbs.map { verb -> verb.infinitive }).toImmutableSet(),
                    canRestore = hasDeletedVerbs(),
                )
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        viewModelScope.launch(dispatchers.io) {
            val matches = loadVerbs(query)
            val starred = loadStudySet(matches.map { it.infinitive })
            _uiState.update { it.copy(verbs = matches, studySet = starred.toImmutableSet()) }
        }
    }

    fun onVerbToggled(infinitive: String) =
        _uiState.update { state ->
            val next = if (infinitive in state.selected) state.selected - infinitive else state.selected + infinitive
            state.copy(selected = next.toImmutableSet(), isSaved = false)
        }

    fun onStudySetToggled(verb: VerbConjugation) {
        val next = verb.infinitive !in _uiState.value.studySet
        _uiState.update { state ->
            val studySet = if (next) state.studySet + verb.infinitive else state.studySet - verb.infinitive
            state.copy(studySet = studySet.toImmutableSet())
        }
        viewModelScope.launch(dispatchers.io) {
            studySetVerb(verb.infinitive, verb.translation, next)
        }
    }

    fun onCreateCourse() {
        val chosen = _uiState.value.selected.toList()
        viewModelScope.launch(dispatchers.io) {
            createCourse(chosen)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun onVerbDeleted(infinitive: String) {
        viewModelScope.launch(dispatchers.io) {
            deleteVerb(infinitive)
            refresh()
        }
    }

    fun onRestoreAll() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(dispatchers.io) {
            restoreVerbs()
            refresh()
        }
    }

    private suspend fun refresh() {
        val verbs = loadVerbs(_uiState.value.query)
        val canRestore = hasDeletedVerbs()
        _uiState.update {
            it.copy(
                isLoading = false,
                canRestore = canRestore,
                verbs = verbs,
                selected = it.selected.filter { chosen -> verbs.any { v -> v.infinitive == chosen } }.toImmutableSet(),
            )
        }
    }

    fun onSaveHandled() = _uiState.update { it.copy(isSaved = false) }
}
