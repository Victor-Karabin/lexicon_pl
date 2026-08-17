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
    /** How many words the study set holds, which is what the program is over. */
    val favourites: Int = 0,
    val languageTag: String = "en",
    val day: ProgramDay? = null,
    /** Set when a session is ready; the screen navigates on it and clears it. */
    val launch: LaunchTraining? = null,
    /** Set when today starts with new words to meet. */
    val openCards: Boolean = false,
) {
    val trainingsDone: Int get() = day?.completedTrainings ?: 0

    val trainingsTotal: Int get() = day?.totalTrainings ?: 0

    val isDayComplete: Boolean get() = day?.isComplete == true

    /** How much of today's queue is behind the learner, for the ring around the heart. */
    val trainingsFraction: Float
        get() = if (trainingsTotal <= 0) 0f else trainingsDone.toFloat() / trainingsTotal
}

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

    /** Recomputes after a session, when the numbers behind it have moved. */
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

    /**
     * The next thing in the day: the new-word cards if they have not been through,
     * otherwise the next training in the queue that has no session against it yet.
     */
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
            // The words come from the same session builder as before; only which
            // training runs them is the queue's business.
            val session = startSession(program.id)
            _uiState.update {
                it.copy(launch = LaunchTraining(next.training, session?.wordIds ?: persistentListOf()))
            }
        }
    }

    fun onLaunchHandled() = _uiState.update { it.copy(launch = null, openCards = false) }
}
