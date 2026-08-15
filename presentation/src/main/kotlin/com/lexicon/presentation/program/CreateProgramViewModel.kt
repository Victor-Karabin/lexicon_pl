package com.lexicon.presentation.program

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.interactors.program.CountFavouritesUseCase
import com.lexicon.interactors.program.CreateProgramUseCase
import com.lexicon.interactors.program.EnrolInProgramUseCase
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.LeaveProgramUseCase
import com.lexicon.interactors.program.ObserveActiveEnrolmentUseCase
import com.lexicon.interactors.program.ProgramDraft
import com.lexicon.interactors.program.ProgramDraftException
import com.lexicon.interactors.program.ProgramDraftProblem
import com.lexicon.interactors.program.ProgramId
import com.lexicon.interactors.program.UpdateProgramUseCase
import com.lexicon.presentation.main.programTrainings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Route argument naming the program being edited. */
const val PROGRAM_ID_ARG = "programId"

/** The floor the form offers for new words a day, and what it starts at. */
const val MIN_NEW_WORDS_A_DAY = 10

private const val DEFAULT_REVIEW_WORDS_A_DAY = 20

data class CreateProgramUiState(
    val isLoading: Boolean = true,
    val favourites: Int = 0,
    val newWordsPerDay: Int = MIN_NEW_WORDS_A_DAY,
    val reviewWordsPerDay: Int = DEFAULT_REVIEW_WORDS_A_DAY,
    /** The chosen trainings, in the order the day runs them. */
    val queue: ImmutableList<String> = persistentListOf(),
    val problem: ProgramDraftProblem? = null,
    val isSaved: Boolean = false,
    /** Rewriting one that exists rather than writing a new one. */
    val isEditing: Boolean = false,
    /** Whether this is the program currently being worked through. */
    val isEnrolled: Boolean = false,
) {
    /** A study set smaller than the floor still deserves a program over it. */
    val maxNewWords: Int get() = maxOf(favourites, MIN_NEW_WORDS_A_DAY)

    val hasFavourites: Boolean get() = favourites > 0

    val canSave: Boolean get() = queue.isNotEmpty() && hasFavourites
}

/**
 * A program over the words the learner starred.
 *
 * The form asks only what is worth choosing — how much a day, and which trainings in
 * what order. Its name is fixed: there is one program and it is over the study set,
 * so naming it is a field to fill in rather than a decision. The goal, the scope and
 * the weights follow from the study set itself.
 */
class CreateProgramViewModel(
    savedStateHandle: SavedStateHandle,
    private val createProgram: CreateProgramUseCase,
    private val updateProgram: UpdateProgramUseCase,
    private val getProgram: GetProgramUseCase,
    private val countFavourites: CountFavouritesUseCase,
    private val enrol: EnrolInProgramUseCase,
    private val leave: LeaveProgramUseCase,
    observeActiveEnrolment: ObserveActiveEnrolmentUseCase,
) : ViewModel() {
    /** Absent when the form is writing a new program rather than editing one. */
    private val editing: ProgramId? = savedStateHandle.get<String>(PROGRAM_ID_ARG)?.let(::ProgramId)

    private val _uiState = MutableStateFlow(CreateProgramUiState())
    val uiState: StateFlow<CreateProgramUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val favourites = countFavourites()
            val ceiling = maxOf(favourites, MIN_NEW_WORDS_A_DAY)
            // What the program already says, so an edit starts from it rather than
            // from the defaults — a form that forgets is a form that overwrites.
            val existing = editing?.let { getProgram(it) }
            val plan = existing?.config?.dailyPlan

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    isEditing = existing != null,
                    favourites = favourites,
                    newWordsPerDay = (plan?.newWords ?: state.newWordsPerDay).coerceIn(MIN_NEW_WORDS_A_DAY, ceiling),
                    reviewWordsPerDay = (plan?.reviewWords ?: state.reviewWordsPerDay).coerceIn(0, ceiling),
                    queue = plan?.queue?.toImmutableList() ?: state.queue,
                )
            }
        }

        // Starting and stopping live here too: this is the only screen a program has,
        // so it has to be the one that answers "am I on this?".
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

    /**
     * Picking a training adds a turn at it.
     *
     * Adds rather than toggles, because the same training twice in the queue is two
     * turns at it — which is how the length of a day is chosen now.
     */
    fun onTrainingAdded(id: String) =
        _uiState.update { state ->
            state.copy(queue = (state.queue + id).toImmutableList(), problem = null)
        }

    /** Drops one turn, the one at [index], rather than every turn at that training. */
    fun onTurnRemoved(index: Int) =
        _uiState.update { state ->
            if (index !in state.queue.indices) return@update state
            state.copy(queue = (state.queue.toMutableList().apply { removeAt(index) }).toImmutableList())
        }

    fun onMoveEarlier(index: Int) = move(index, -1)

    fun onMoveLater(index: Int) = move(index, 1)

    /** By position, not by name: the same training can sit in the queue more than once. */
    private fun move(
        from: Int,
        by: Int,
    ) = _uiState.update { state ->
        val to = from + by
        if (from !in state.queue.indices || to !in state.queue.indices) return@update state
        val queue = state.queue.toMutableList()
        queue[from] = queue[to].also { queue[to] = queue[from] }
        state.copy(queue = queue.toImmutableList())
    }

    /** [name] and [description] are the app's own words for it, passed in by the screen that owns the copy. */
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

    /** Every training a program may use, whether or not it is in the queue yet. */
    val available: List<String> get() = programTrainings.map { it.id }
}
