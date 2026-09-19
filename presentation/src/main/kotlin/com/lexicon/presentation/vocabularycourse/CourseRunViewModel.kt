package com.lexicon.presentation.vocabularycourse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.vocabularycourse.GetVocabularyCourseUseCase
import com.lexicon.interactors.vocabularycourse.NextCourseTrainingUseCase
import com.lexicon.interactors.vocabularycourse.ResetCourseQueueUseCase
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CourseRunStep {
    data object Idle : CourseRunStep

    data object Working : CourseRunStep

    data class Next(
        val training: TrainingType,
        val wordIds: ImmutableList<VocabularyId>,
    ) : CourseRunStep

    data object Cards : CourseRunStep

    data object NothingToPractise : CourseRunStep
}

class CourseRunViewModel(
    private val queue: NextCourseTrainingUseCase,
    private val resetQueue: ResetCourseQueueUseCase,
    private val getCourse: GetVocabularyCourseUseCase,
) : ViewModel() {
    private val _step = MutableStateFlow<CourseRunStep>(CourseRunStep.Idle)
    val step: StateFlow<CourseRunStep> = _step.asStateFlow()

    fun onTrainingFinished() = run { queue.advance().toStep() }

    fun onReset() =
        run {
            resetQueue()
            if (getCourse().showCardsNext) CourseRunStep.Cards else queue.next().toStep()
        }

    fun onStepHandled() = _step.update { CourseRunStep.Idle }

    private fun run(work: suspend () -> CourseRunStep) {
        if (_step.value != CourseRunStep.Idle) return
        _step.value = CourseRunStep.Working
        viewModelScope.launch { _step.value = work() }
    }

    private fun com.lexicon.interactors.vocabularycourse.CourseLaunch?.toStep(): CourseRunStep =
        when (this) {
            null -> CourseRunStep.NothingToPractise
            else -> CourseRunStep.Next(training = training, wordIds = wordIds)
        }
}
