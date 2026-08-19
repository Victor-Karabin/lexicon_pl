package com.lexicon.presentation.program

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.program.CountStudySetUseCase
import com.lexicon.interactors.program.CreateProgramUseCase
import com.lexicon.interactors.program.EnrolInProgramUseCase
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.LeaveProgramUseCase
import com.lexicon.interactors.program.ObserveActiveEnrolmentUseCase
import com.lexicon.interactors.program.ProgramDraft
import com.lexicon.interactors.program.ProgramDraftException
import com.lexicon.interactors.program.ProgramDraftProblem
import com.lexicon.interactors.program.UpdateProgramUseCase
import com.lexicon.model.program.ProgramId
import com.lexicon.presentation.main.programTrainings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val PROGRAM_ID_ARG = "programId"

const val MIN_NEW_WORDS_A_DAY = 10

private const val DEFAULT_REVIEW_WORDS_A_DAY = 20

data class CreateProgramUiState(
    val isLoading: Boolean = true,
    val studySet: Int = 0,
    val newWordsPerDay: Int = MIN_NEW_WORDS_A_DAY,
    val reviewWordsPerDay: Int = DEFAULT_REVIEW_WORDS_A_DAY,
    val queue: ImmutableList<String> = persistentListOf(),
    val problem: ProgramDraftProblem? = null,
    val isSaved: Boolean = false,
    val isEditing: Boolean = false,
    val isEnrolled: Boolean = false,
) {
    val maxNewWords: Int get() = maxOf(studySet, MIN_NEW_WORDS_A_DAY)

    val hasStudySet: Boolean get() = studySet > 0

    val canSave: Boolean get() = queue.isNotEmpty() && hasStudySet
}

class CreateProgramViewModel(
    savedStateHandle: SavedStateHandle,
    private val createProgram: CreateProgramUseCase,
    private val updateProgram: UpdateProgramUseCase,
    private val getProgram: GetProgramUseCase,
    private val countStudySet: CountStudySetUseCase,
    private val enrol: EnrolInProgramUseCase,
    private val leave: LeaveProgramUseCase,
    observeActiveEnrolment: ObserveActiveEnrolmentUseCase,
) : ViewModel() {
    private val editing: ProgramId? = savedStateHandle.get<String>(PROGRAM_ID_ARG)?.let(::ProgramId)

    private val _uiState = MutableStateFlow(CreateProgramUiState())
    val uiState: StateFlow<CreateProgramUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val studySet = countStudySet()
            val ceiling = maxOf(studySet, MIN_NEW_WORDS_A_DAY)

            val existing = editing?.let { getProgram(it) }
            val plan = existing?.config?.dailyPlan

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    isEditing = existing != null,
                    studySet = studySet,
                    newWordsPerDay = (plan?.newWords ?: state.newWordsPerDay).coerceIn(MIN_NEW_WORDS_A_DAY, ceiling),
                    reviewWordsPerDay = (plan?.reviewWords ?: state.reviewWordsPerDay).coerceIn(0, ceiling),
                    queue = plan?.queue?.toImmutableList() ?: state.queue,
                )
            }
        }

        viewModelScope.launch {
            observeActiveEnrolment().collect { active ->
                val mine = active != null && active.programId == editing
                _uiState.update { it.copy(isEnrolled = mine) }
            }
        }
    }

    fun onEnrolToggled() {
        val id = editing ?: return
        viewModelScope.launch {
            if (_uiState.value.isEnrolled) leave(id) else enrol(id)
        }
    }

    fun onNewWordsChanged(value: Int) = _uiState.update { it.copy(newWordsPerDay = value) }

    fun onReviewWordsChanged(value: Int) = _uiState.update { it.copy(reviewWordsPerDay = value) }

    fun onTrainingAdded(id: String) =
        _uiState.update { state ->
            state.copy(queue = (state.queue + id).toImmutableList(), problem = null)
        }

    fun onTurnRemoved(index: Int) =
        _uiState.update { state ->
            if (index !in state.queue.indices) return@update state
            state.copy(queue = (state.queue.toMutableList().apply { removeAt(index) }).toImmutableList())
        }

    fun onMove(
        from: Int,
        to: Int,
    ) = _uiState.update { state ->
        if (from !in state.queue.indices || to !in state.queue.indices) return@update state
        val queue = state.queue.toMutableList()
        queue.add(to, queue.removeAt(from))
        state.copy(queue = queue.toImmutableList())
    }

    fun onSave(
        name: String,
        description: String,
    ) {
        val state = _uiState.value
        val draft = ProgramDraft(
            title = name,
            description = description,
            newWordsPerDay = state.newWordsPerDay,
            reviewWordsPerDay = state.reviewWordsPerDay,
            trainings = state.queue,
        )
        viewModelScope.launch {
            val saved = editing?.let { updateProgram(it, draft) } ?: createProgram(draft)
            saved.onSuccess {
                _uiState.update { it.copy(isSaved = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(problem = (error as? ProgramDraftException)?.problem) }
            }
        }
    }

    val available: List<String> get() = programTrainings.map { it.id }
}
