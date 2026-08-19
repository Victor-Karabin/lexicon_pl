package com.lexicon.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.conjugation.ConjugationCourse
import com.lexicon.interactors.conjugation.DeleteConjugationCourseUseCase
import com.lexicon.interactors.conjugation.LoadConjugationCoursesUseCase
import com.lexicon.interactors.program.CountStudySetUseCase
import com.lexicon.interactors.program.GetProgramDayUseCase
import com.lexicon.interactors.program.GetProgramProgressUseCase
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.GetStudyStreakUseCase
import com.lexicon.interactors.program.NextProgramTrainingUseCase
import com.lexicon.interactors.program.ObserveActiveEnrolmentUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramDay
import com.lexicon.model.program.ProgramProgress
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LaunchTraining(
    val training: TrainingType,
    val wordIds: ImmutableList<VocabularyId>,
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val program: Program? = null,
    val progress: ProgramProgress? = null,
    val streakDays: Int = 0,
    val studySet: Int = 0,
    val languageTag: String = "en",
    val day: ProgramDay? = null,
    val launch: LaunchTraining? = null,
    val openCards: Boolean = false,
    val conjugationCourses: ImmutableList<ConjugationCourse> = persistentListOf(),
) {
    val hasConjugationCourse: Boolean get() = conjugationCourses.isNotEmpty()

    val trainingsDone: Int get() = day?.completedTrainings ?: 0

    val trainingsTotal: Int get() = day?.totalTrainings ?: 0

    val isDayComplete: Boolean get() = day?.isComplete == true

    val trainingsFraction: Float
        get() = if (trainingsTotal <= 0) 0f else trainingsDone.toFloat() / trainingsTotal
}

class DashboardViewModel(
    private val getProgram: GetProgramUseCase,
    private val getProgress: GetProgramProgressUseCase,
    private val queue: NextProgramTrainingUseCase,
    private val loadConjugationCourses: LoadConjugationCoursesUseCase,
    private val deleteConjugationCourse: DeleteConjugationCourseUseCase,
    private val getDay: GetProgramDayUseCase,
    private val getStreak: GetStudyStreakUseCase,
    private val countStudySet: CountStudySetUseCase,
    observeActiveEnrolment: ObserveActiveEnrolmentUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeActiveEnrolment().collect { enrolment ->
                val courses = loadConjugationCourses()
                val program = enrolment?.let { getProgram(it.programId) }

                _uiState.value = DashboardUiState(
                    isLoading = false,
                    conjugationCourses = courses,
                    program = program,
                    progress = program?.let { getProgress(it) },
                    streakDays = if (program == null) 0 else getStreak(),
                    studySet = if (program == null) 0 else countStudySet(),
                    day = program?.let { getDay(it.id) },
                )
            }
        }
    }

    fun onResumed() {
        val program = _uiState.value.program ?: return
        viewModelScope.launch {
            val day = getDay(program.id)
            _uiState.update {
                it.copy(
                    progress = getProgress(program),
                    streakDays = getStreak(),
                    studySet = countStudySet(),
                    day = day,
                )
            }
        }
    }

    fun onContinue() {
        val state = _uiState.value
        val program = state.program ?: return
        val day = state.day ?: return

        if (day.showCardsNext) {
            _uiState.update { it.copy(openCards = true) }
            return
        }
        viewModelScope.launch {
            val next = queue.next(program.id) ?: return@launch
            _uiState.update { it.copy(launch = LaunchTraining(next.training, next.wordIds)) }
        }
    }

    fun onLaunchHandled() = _uiState.update { it.copy(launch = null, openCards = false) }

    fun refreshConjugation() {
        viewModelScope.launch {
            _uiState.update { it.copy(conjugationCourses = loadConjugationCourses()) }
        }
    }

    fun onConjugationCourseRemoved(courseId: String) {
        viewModelScope.launch {
            deleteConjugationCourse(courseId)
            _uiState.update { it.copy(conjugationCourses = loadConjugationCourses()) }
        }
    }
}
