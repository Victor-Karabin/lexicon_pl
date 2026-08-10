package com.lexicon.presentation.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.course.Course
import com.lexicon.interactors.course.ObserveCoursesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface CourseUiState {
    data object Loading : CourseUiState

    data class Loaded(
        val courses: ImmutableList<Course>,
        val languageTag: String = "en",
    ) : CourseUiState
}

val CourseUiState.Loaded.isEmpty: Boolean
    get() = courses.all { it.lessons.isEmpty() }

@HiltViewModel
class CourseViewModel
    @Inject
    constructor(
        observeCourses: ObserveCoursesUseCase,
    ) : ViewModel() {
        val uiState: StateFlow<CourseUiState> =
            observeCourses()
                .map { CourseUiState.Loaded(it) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    initialValue = CourseUiState.Loading,
                )

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
