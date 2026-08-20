package com.lexicon.presentation.program

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.program.NextProgramTrainingUseCase
import com.lexicon.model.program.ProgramId
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ProgramRunStep {
    data object Idle : ProgramRunStep

    data object Working : ProgramRunStep

    data class Next(
        val training: TrainingType,
        val wordIds: ImmutableList<VocabularyId>,
    ) : ProgramRunStep

    data object DayComplete : ProgramRunStep
}

class ProgramRunViewModel(
    private val queue: NextProgramTrainingUseCase,
) : ViewModel() {
    private val _step = MutableStateFlow<ProgramRunStep>(ProgramRunStep.Idle)
    val step: StateFlow<ProgramRunStep> = _step.asStateFlow()

    fun onTrainingFinished(programId: String) {
        if (_step.value != ProgramRunStep.Idle) return
        _step.value = ProgramRunStep.Working

        viewModelScope.launch {
            val launch = queue.advance(ProgramId(programId))
            _step.value = when (launch) {
                null -> ProgramRunStep.DayComplete
                else -> ProgramRunStep.Next(training = launch.training, wordIds = launch.wordIds)
            }
        }
    }

    fun onStepHandled() = _step.update { ProgramRunStep.Idle }
}
