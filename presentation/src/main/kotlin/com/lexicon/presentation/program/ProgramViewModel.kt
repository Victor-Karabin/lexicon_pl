package com.lexicon.presentation.program

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.program.EnrolInProgramUseCase
import com.lexicon.interactors.program.EnrolmentStatus
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.LeaveProgramUseCase
import com.lexicon.interactors.program.ObserveActiveEnrolmentUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramEnrolment
import com.lexicon.interactors.program.ProgramId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Route argument naming the program being looked at. */
const val PROGRAM_ID_ARG = "programId"

sealed interface ProgramUiState {
    data object Loading : ProgramUiState

    data object NotFound : ProgramUiState

    data class Loaded(
        val program: Program,
        val enrolment: ProgramEnrolment? = null,
        val languageTag: String = "en",
    ) : ProgramUiState {
        val isEnrolled: Boolean get() = enrolment?.status == EnrolmentStatus.ACTIVE
    }
}

/**
 * One program: what it asks of the learner, and whether they have taken it on.
 *
 * Everything shown here is read straight from the configuration — the goal, the
 * daily shape, the milestone ladder. None of it needs the engine, which is why the
 * screen can exist before the engine does.
 */
class ProgramViewModel(
    savedStateHandle: SavedStateHandle,
    private val getProgram: GetProgramUseCase,
    private val enrol: EnrolInProgramUseCase,
    private val leave: LeaveProgramUseCase,
    observeActiveEnrolment: ObserveActiveEnrolmentUseCase,
) : ViewModel() {
    private val programId = ProgramId(savedStateHandle.get<String>(PROGRAM_ID_ARG).orEmpty())

    private val _uiState = MutableStateFlow<ProgramUiState>(ProgramUiState.Loading)
    val uiState: StateFlow<ProgramUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val program = getProgram(programId)
            _uiState.value = if (program == null) {
                ProgramUiState.NotFound
            } else {
                ProgramUiState.Loaded(program = program)
            }
        }
        viewModelScope.launch {
            observeActiveEnrolment().collect { active ->
                // Only the enrolment for this program; another being active says
                // nothing about this one beyond that it is not.
                val mine = active?.takeIf { it.programId == programId }
                _uiState.update { state ->
                    if (state is ProgramUiState.Loaded) state.copy(enrolment = mine) else state
                }
            }
        }
    }

    fun onEnrol() {
        viewModelScope.launch { enrol(programId) }
    }

    fun onLeave() {
        viewModelScope.launch { leave(programId) }
    }
}
