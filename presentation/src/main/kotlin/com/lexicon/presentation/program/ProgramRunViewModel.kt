package com.lexicon.presentation.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.program.AdvanceProgramDayUseCase
import com.lexicon.interactors.program.ProgramId
import com.lexicon.interactors.program.StartProgramSessionUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ProgramRunStep {
    data object Idle : ProgramRunStep

    data object Working : ProgramRunStep

    data class Next(
        val training: String,
        val wordIds: ImmutableList<VocabularyId>,
    ) : ProgramRunStep

    data object DayComplete : ProgramRunStep
}

class ProgramRunViewModel(
    private val advanceDay: AdvanceProgramDayUseCase,
    private val startSession: StartProgramSessionUseCase,
) : ViewModel() {
    private val _step = MutableStateFlow<ProgramRunStep>(ProgramRunStep.Idle)
    val step: StateFlow<ProgramRunStep> = _step.asStateFlow()

    fun onTrainingFinished(programId: String) {
        if (_step.value != ProgramRunStep.Idle) return
        _step.value = ProgramRunStep.Working

        viewModelScope.launch {
            val id = ProgramId(programId)
            val day = advanceDay(id)
            val next = day?.nextTraining
            _step.value = when {
                day == null || next == null -> ProgramRunStep.DayComplete
                else -> ProgramRunStep.Next(
                    training = next.training,
                    wordIds = startSession(id)?.wordIds ?: persistentListOf(),
                )
            }
        }
    }

    fun onStepHandled() = _step.update { ProgramRunStep.Idle }
}
