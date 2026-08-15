package com.lexicon.presentation.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.course.Course
import com.lexicon.interactors.course.ObserveCoursesUseCase
import com.lexicon.interactors.program.ObserveActiveEnrolmentUseCase
import com.lexicon.interactors.program.ObserveProgramsUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramEnrolment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface PlanUiState {
    data object Loading : PlanUiState

    data class Loaded(
        val programs: ImmutableList<Program> = persistentListOf(),
        val courses: ImmutableList<Course> = persistentListOf(),
        val activeEnrolment: ProgramEnrolment? = null,
        val languageTag: String = "en",
    ) : PlanUiState
}

/** True only when there is nothing at all to show, not merely no courses. */
val PlanUiState.Loaded.isEmpty: Boolean
    get() = programs.isEmpty() && courses.all { it.lessons.isEmpty() }

/**
 * The Plan tab: the programs on offer and the courses to work through.
 *
 * Two kinds of thing sit here — a program is a configured path toward a goal, a
 * course is a book's worth of lessons — so the tab combines them rather than either
 * owning the other.
 */
class PlanViewModel(
    observeCourses: ObserveCoursesUseCase,
    observePrograms: ObserveProgramsUseCase,
    observeActiveEnrolment: ObserveActiveEnrolmentUseCase,
) : ViewModel() {
    val uiState: StateFlow<PlanUiState> =
        combine(
            observePrograms(),
            observeCourses(),
            observeActiveEnrolment(),
        ) { programs, courses, enrolment ->
            PlanUiState.Loaded(programs = programs, courses = courses, activeEnrolment = enrolment)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = PlanUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
