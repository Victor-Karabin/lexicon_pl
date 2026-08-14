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

/** Route argument naming the course whose lessons are being shown. */
const val COURSE_ID_ARG = "courseId"

sealed interface CourseDetailUiState {
    data object Loading : CourseDetailUiState

    /** The id names no course the catalogue still ships. */
    data object NotFound : CourseDetailUiState

    data class Loaded(
        val course: Course,
        val languageTag: String = "en",
    ) : CourseDetailUiState
}

/**
 * One course's lessons.
 *
 * Reads the same stream the Plan tab does and picks its course out of it, rather
 * than asking for one by id: lesson progress already arrives that way, so a lesson
 * marked done updates here without anything else being wired up.
 */
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
