package com.lexicon.presentation.course

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.runSuspendCatching
import com.lexicon.interactors.course.ObserveCoursesUseCase
import com.lexicon.interactors.course.SetCourseOnDashboardUseCase
import com.lexicon.interactors.vocabularycourse.ObserveVocabularyCourseUseCase
import com.lexicon.interactors.vocabularycourse.VocabularyCourse
import com.lexicon.model.course.Course
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private val setCourseOnDashboard: SetCourseOnDashboardUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    val uiState: StateFlow<PlanUiState> =
        combine(observeCourse(), observeCourses()) { course, courses ->
            PlanUiState.Loaded(course = course, courses = courses)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = PlanUiState.Loading,
        )

    fun onPinToggled(course: Course) {
        viewModelScope.launch(dispatchers.io) {
            runSuspendCatching { setCourseOnDashboard(course.id, !course.isOnDashboard) }
                .onFailure { Log.w(TAG, "The course could not be pinned to or unpinned from the dashboard", it) }
        }
    }

    private companion object {
        const val TAG = "Plan"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
