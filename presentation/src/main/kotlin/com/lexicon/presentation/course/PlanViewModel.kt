package com.lexicon.presentation.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.course.ObserveCoursesUseCase
import com.lexicon.interactors.program.ObserveActiveProgramUseCase
import com.lexicon.interactors.program.ObserveProgramsUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.model.course.Course
import com.lexicon.model.program.ProgramId
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
        val activeProgramId: ProgramId? = null,
        val languageTag: String = "en",
    ) : PlanUiState
}

val PlanUiState.Loaded.isEmpty: Boolean
    get() = programs.isEmpty() && courses.all { it.lessons.isEmpty() }

class PlanViewModel(
    observeCourses: ObserveCoursesUseCase,
    observePrograms: ObserveProgramsUseCase,
    observeActiveProgram: ObserveActiveProgramUseCase,
) : ViewModel() {
    val uiState: StateFlow<PlanUiState> =
        combine(
            observePrograms(),
            observeCourses(),
            observeActiveProgram(),
        ) { programs, courses, active ->
            PlanUiState.Loaded(programs = programs, courses = courses, activeProgramId = active?.id)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = PlanUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
