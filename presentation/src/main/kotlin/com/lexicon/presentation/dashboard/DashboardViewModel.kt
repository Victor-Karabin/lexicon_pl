package com.lexicon.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.program.GetProgramProgressUseCase
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.GetStudyStreakUseCase
import com.lexicon.interactors.program.ObserveActiveEnrolmentUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramProgress
import com.lexicon.interactors.program.StartProgramSessionUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where to send the learner when they tap the program: a training over these words. */
data class LaunchTraining(
    val training: String,
    val wordIds: ImmutableList<VocabularyId>,
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val program: Program? = null,
    val progress: ProgramProgress? = null,
    val streakDays: Int = 0,
    val languageTag: String = "en",
    /** Set when a session is ready; the screen navigates on it and clears it. */
    val launch: LaunchTraining? = null,
    /** The program has nothing left to do today. */
    val nothingDueToday: Boolean = false,
)

/**
 * What the learner should see first: the program they are on and how it is going.
 *
 * Progress is recomputed whenever the screen is shown rather than observed, because
 * every figure behind it — words mastered, retention, days studied — moves only when
 * a session ends, and the learner is on a training screen when that happens.
 */
class DashboardViewModel(
    private val getProgram: GetProgramUseCase,
    private val getProgress: GetProgramProgressUseCase,
    private val startSession: StartProgramSessionUseCase,
    private val getStreak: GetStudyStreakUseCase,
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
                )
            }
        }
    }

    /** Recomputes after a session, when the numbers behind it have moved. */
    fun onResumed() {
        val program = _uiState.value.program ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    progress = getProgress(program),
                    streakDays = getStreak(),
                    nothingDueToday = false,
                )
            }
        }
    }

    fun onStartTraining() {
        val program = _uiState.value.program ?: return
        viewModelScope.launch {
            val session = startSession(program.id)
            _uiState.update {
                if (session == null) {
                    it.copy(nothingDueToday = true)
                } else {
                    it.copy(launch = LaunchTraining(session.training, session.wordIds))
                }
            }
        }
    }

    fun onLaunchHandled() = _uiState.update { it.copy(launch = null) }
}
