package com.lexicon.presentation.numbers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.numbers.LoadNumberSpecUseCase
import com.lexicon.interactors.numbers.NumberSelection
import com.lexicon.interactors.numbers.SaveNumberCourseUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NumberCourseUiState(
    val spec: String = "",
    val selection: NumberSelection = NumberSelection.Empty,
    val isSaving: Boolean = false,
) {
    val canStart: Boolean get() = !isSaving && selection is NumberSelection.Valid
}

class NumberCourseViewModel(
    private val loadSpec: LoadNumberSpecUseCase,
    private val saveCourse: SaveNumberCourseUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NumberCourseUiState())
    val uiState: StateFlow<NumberCourseUiState> = _uiState.asStateFlow()

    private val _started = MutableSharedFlow<Unit>()
    val started: SharedFlow<Unit> = _started.asSharedFlow()

    private var parsing: Job? = null

    init {
        viewModelScope.launch(dispatchers.io) {
            val spec = loadSpec()
            _uiState.update { it.copy(spec = spec, selection = NumberSelection.parse(spec)) }
        }
    }

    fun onSpecChanged(spec: String) {
        _uiState.update { it.copy(spec = spec) }
        parsing?.cancel()
        parsing = viewModelScope.launch(dispatchers.io) {
            val selection = NumberSelection.parse(spec)
            _uiState.update { if (it.spec == spec) it.copy(selection = selection) else it }
        }
    }

    fun onStart() {
        val state = _uiState.value
        if (!state.canStart) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch(dispatchers.io) {
            val selection = saveCourse(state.spec)
            _uiState.update { it.copy(isSaving = false, selection = selection) }
            if (selection is NumberSelection.Valid) _started.emit(Unit)
        }
    }
}
