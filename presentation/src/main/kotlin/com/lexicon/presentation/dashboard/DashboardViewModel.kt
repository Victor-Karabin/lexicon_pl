package com.lexicon.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.program.CountFavouritesUseCase
import com.lexicon.interactors.program.GetProgramDayUseCase
import com.lexicon.interactors.program.GetProgramProgressUseCase
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.GetStudyStreakUseCase
import com.lexicon.interactors.program.ObserveActiveEnrolmentUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramDay
import com.lexicon.interactors.program.ProgramProgress
import com.lexicon.interactors.program.StartProgramSessionUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LaunchTraining(
    val training: String,
    val wordIds: ImmutableList<VocabularyId>,
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val program: Program? = null,
    val progress: ProgramProgress? = null,
    val streakDays: Int = 0,
    val favourites: Int = 0,
    val languageTag: String = "en",
    val day: ProgramDay? = null,
    val launch: LaunchTraining? = null,
    val openCards: Boolean = false,
) {
    val trainingsDone: Int get() = day?.completedTrainings ?: 0

    val trainingsTotal: Int get() = day?.totalTrainings ?: 0

    val isDayComplete: Boolean get() = day?.isComplete == true

    val trainingsFraction: Float
        get() = if (trainingsTotal <= 0) 0f else trainingsDone.toFloat() / trainingsTotal
}

class DashboardViewModel(
    private val getProgram: GetProgramUseCase,
    private val getProgress: GetProgramProgressUseCase,
    private val startSession: StartProgramSessionUseCase,
    private val getDay: GetProgramDayUseCase,
    private val getStreak: GetStudyStreakUseCase,
    private val countFavourites: CountFavouritesUseCase,
    observeActiveEnrolment: ObserveActiveEnrolmentUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeActiveEnrolment().collect { enrolment ->
                if (enrolment == null) {
                    _uiState.value = DashboardUiState(isLoading = false)
                    return@collect
                }
                val program = getProgram(enrolment.programId)
                _uiState.value = DashboardUiState(
                    isLoading = false,
                    program = program,
                    progress = program?.let { getProgress(it) },
                    streakDays = getStreak(),
                    favourites = countFavourites(),
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
                    favourites = countFavourites(),
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
        val next = day.nextTraining ?: return

        viewModelScope.launch {
            val session = startSession(program.id)
            _uiState.update {
                it.copy(launch = LaunchTraining(next.training, session?.wordIds ?: persistentListOf()))
            }
        }
    }

    fun onLaunchHandled() = _uiState.update { it.copy(launch = null, openCards = false) }
}
