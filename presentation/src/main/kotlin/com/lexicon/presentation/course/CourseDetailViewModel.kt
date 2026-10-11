package com.lexicon.presentation.course

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.runSuspendCatching
import com.lexicon.interactors.course.ObserveCoursesUseCase
import com.lexicon.interactors.course.ResetLessonProgressUseCase
import com.lexicon.model.course.Course
import com.lexicon.model.course.CourseId
import com.lexicon.model.course.LessonId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private val resetLessonProgress: ResetLessonProgressUseCase,
    private val dispatchers: DispatcherProvider,
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

    fun onLessonReset(id: LessonId) {
        viewModelScope.launch(dispatchers.io) {
            runSuspendCatching { resetLessonProgress(id) }
                .onFailure { Log.w(TAG, "The lesson's progress could not be reset", it) }
        }
    }

    private companion object {
        const val TAG = "CourseDetail"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
