package com.lexicon.presentation.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.course.ObserveCoursesUseCase
import com.lexicon.interactors.vocabularycourse.ObserveVocabularyCourseUseCase
import com.lexicon.interactors.vocabularycourse.VocabularyCourse
import com.lexicon.model.course.Course
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface PlanUiState {
    data object Loading : PlanUiState

    data class Loaded(
        val course: VocabularyCourse = VocabularyCourse(),
        val courses: ImmutableList<Course> = persistentListOf(),
        val languageTag: String = "en",
    ) : PlanUiState
}

class PlanViewModel(
    observeCourses: ObserveCoursesUseCase,
    observeCourse: ObserveVocabularyCourseUseCase,
) : ViewModel() {
    val uiState: StateFlow<PlanUiState> =
        combine(observeCourse(), observeCourses()) { course, courses ->
            PlanUiState.Loaded(course = course, courses = courses)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = PlanUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
