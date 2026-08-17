package com.lexicon.presentation.course

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.course.Course
import com.lexicon.interactors.course.CourseId
import com.lexicon.interactors.course.ObserveCoursesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

const val COURSE_ID_ARG = "courseId"

sealed interface CourseDetailUiState {
    data object Loading : CourseDetailUiState

    data object NotFound : CourseDetailUiState

    data class Loaded(
        val course: Course,
        val languageTag: String = "en",
    ) : CourseDetailUiState
}

class CourseDetailViewModel(
    savedStateHandle: SavedStateHandle,
    observeCourses: ObserveCoursesUseCase,
) : ViewModel() {
    private val courseId = CourseId(savedStateHandle.get<String>(COURSE_ID_ARG).orEmpty())

    val uiState: StateFlow<CourseDetailUiState> =
        observeCourses()
            .map { courses ->
                courses
                    .firstOrNull { it.id == courseId }
                    ?.let { CourseDetailUiState.Loaded(it) }
                    ?: CourseDetailUiState.NotFound
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = CourseDetailUiState.Loading,
            )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
