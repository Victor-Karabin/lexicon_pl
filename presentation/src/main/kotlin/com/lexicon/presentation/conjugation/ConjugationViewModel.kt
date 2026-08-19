package com.lexicon.presentation.conjugation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.conjugation.ChooseVerbImageUseCase
import com.lexicon.interactors.conjugation.ConjugationTable
import com.lexicon.interactors.conjugation.GrammaticalPerson
import com.lexicon.interactors.conjugation.LoadVerbImageChoicesUseCase
import com.lexicon.interactors.conjugation.NextConjugationTableUseCase
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerRequest
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerUseCase
import com.lexicon.interactors.settings.AppSettings
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.WordResultEntry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConjugationUiState(
    val isLoading: Boolean = true,
    val table: ConjugationTable? = null,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val answers: ImmutableMap<GrammaticalPerson, String> = persistentMapOf(),
    val correctness: ImmutableMap<GrammaticalPerson, Boolean> = persistentMapOf(),
    val answerState: AnswerState = AnswerState.Unanswered,
    val hasNoVerbs: Boolean = false,
    val isPickingImage: Boolean = false,
    val imageChoices: ImmutableList<String> = persistentListOf(),
) {
    val isAnswered: Boolean get() = answerState !is AnswerState.Unanswered

    val canCheck: Boolean
        get() = !isAnswered && table?.steps?.all { answers.containsKey(it.variant.person) } == true

    val usedOptions: Set<String> get() = answers.values.toSet()
}

const val CONJUGATION_COURSE_ARG = "courseId"

class ConjugationViewModel(
    savedStateHandle: SavedStateHandle,
    private val nextQuestion: NextConjugationTableUseCase,
    private val submitAnswer: SubmitConjugationAnswerUseCase,
    private val loadImageChoices: LoadVerbImageChoicesUseCase,
    private val chooseImage: ChooseVerbImageUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val lastSessionResultsHolder: LastSessionResultsHolder,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val courseId: String = savedStateHandle.get<String>(CONJUGATION_COURSE_ARG).orEmpty()

    private val _uiState = MutableStateFlow(ConjugationUiState())
    val uiState: StateFlow<ConjugationUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
    val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

    private val results = mutableListOf<WordResultEntry>()
    private var correct = 0
    private var incorrect = 0

    init {
        viewModelScope.launch(dispatchers.io) {
            val first = nextQuestion(courseId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    table = first,
                    hasNoVerbs = first == null,
                    totalSteps = AppSettings.DEFAULT_STEP_COUNT,
                )
            }
        }
    }

    /** A tapped option drops into the first row still empty, as the word bank does. */
    fun onOptionPicked(option: String) =
        _uiState.update { state ->
            if (state.isAnswered) return@update state
            val next = state.table?.steps?.firstOrNull { !state.answers.containsKey(it.variant.person) }
                ?: return@update state

            state.copy(answers = (state.answers + (next.variant.person to option)).toImmutableMap())
        }

    fun onRowCleared(person: GrammaticalPerson) =
        _uiState.update { state ->
            if (state.isAnswered) state else state.copy(answers = (state.answers - person).toImmutableMap())
        }

    fun onCheck() {
        val state = _uiState.value
        val table = state.table ?: return
        if (!state.canCheck) return

        viewModelScope.launch(dispatchers.io) {
            val response = submitAnswer(SubmitConjugationAnswerRequest(courseId, table, state.answers))
            val allRight = response.allCorrect

            table.steps.forEach { step ->
                val right = response.correctness[step.variant.person] == true
                if (right) correct++ else incorrect++
                results += WordResultEntry(
                    word = step.spokenForm,
                    translation = "${table.infinitive} · ${step.variant.person.label}",
                    outcome = if (right) AnswerState.Correct else AnswerState.Incorrect(step.correctOptions.first()),
                )
            }

            _uiState.update {
                it.copy(
                    correctness = response.correctness.toImmutableMap(),
                    answerState = if (allRight) AnswerState.Correct else AnswerState.Incorrect(""),
                )
            }
        }
    }

    fun onNext() {
        viewModelScope.launch(dispatchers.io) {
            val state = _uiState.value
            val nextIndex = state.stepIndex + 1

            if (nextIndex >= state.totalSteps) {
                lastSessionResultsHolder.wordResults = results.toList()
                _navigationEvents.emit(SessionNavigationEvent.SessionComplete(correct, incorrect, 0, 0))
                return@launch
            }

            val table = nextQuestion(courseId)
            if (table == null) {
                lastSessionResultsHolder.wordResults = results.toList()
                _navigationEvents.emit(SessionNavigationEvent.SessionComplete(correct, incorrect, 0, 0))
                return@launch
            }

            _uiState.update {
                it.copy(
                    table = table,
                    stepIndex = nextIndex,
                    answers = persistentMapOf(),
                    correctness = persistentMapOf(),
                    answerState = AnswerState.Unanswered,
                )
            }
        }
    }

    fun onSpeak(form: String) {
        if (form.isBlank()) return
        viewModelScope.launch(dispatchers.io) { runCatching { speechSynthesizer.speak(form) } }
    }

    fun onEditVerb() {
        val table = _uiState.value.table ?: return
        _uiState.update { it.copy(isPickingImage = true) }
        viewModelScope.launch(dispatchers.io) {
            val choices = loadImageChoices(table.infinitive, table.translation)
            _uiState.update { it.copy(imageChoices = choices) }
        }
    }

    fun onImageChosen(url: String) {
        val table = _uiState.value.table ?: return
        _uiState.update {
            it.copy(isPickingImage = false, imageChoices = persistentListOf(), table = it.table?.copy(imageUrl = url))
        }
        viewModelScope.launch(dispatchers.io) { chooseImage(table.infinitive, table.translation, url) }
    }

    fun onImagePickerDismissed() = _uiState.update { it.copy(isPickingImage = false, imageChoices = persistentListOf()) }
}
