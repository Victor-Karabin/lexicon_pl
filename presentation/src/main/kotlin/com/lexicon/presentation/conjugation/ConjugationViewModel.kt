package com.lexicon.presentation.conjugation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.android.speech.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.conjugation.ChooseVerbImageUseCase
import com.lexicon.interactors.conjugation.ConjugationQuestion
import com.lexicon.interactors.conjugation.GrammaticalPerson
import com.lexicon.interactors.conjugation.LoadVerbImageChoicesUseCase
import com.lexicon.interactors.conjugation.NextConjugationQuestionUseCase
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
    val question: ConjugationQuestion? = null,
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
        get() = !isAnswered && question?.steps?.all { answers.containsKey(it.variant.person) } == true

    val usedOptions: Set<String> get() = answers.values.toSet()
}

class ConjugationViewModel(
    private val nextQuestion: NextConjugationQuestionUseCase,
    private val submitAnswer: SubmitConjugationAnswerUseCase,
    private val loadImageChoices: LoadVerbImageChoicesUseCase,
    private val chooseImage: ChooseVerbImageUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val lastSessionResultsHolder: LastSessionResultsHolder,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConjugationUiState())
    val uiState: StateFlow<ConjugationUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
    val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

    private val results = mutableListOf<WordResultEntry>()
    private var correct = 0
    private var incorrect = 0

    init {
        viewModelScope.launch(dispatchers.io) {
            val first = nextQuestion()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    question = first,
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
            val next = state.question?.steps?.firstOrNull { !state.answers.containsKey(it.variant.person) }
                ?: return@update state

            state.copy(answers = (state.answers + (next.variant.person to option)).toImmutableMap())
        }

    fun onRowCleared(person: GrammaticalPerson) =
        _uiState.update { state ->
            if (state.isAnswered) state else state.copy(answers = (state.answers - person).toImmutableMap())
        }

    fun onCheck() {
        val state = _uiState.value
        val question = state.question ?: return
        if (!state.canCheck) return

        viewModelScope.launch(dispatchers.io) {
            val response = submitAnswer(SubmitConjugationAnswerRequest(question, state.answers))
            val allRight = response.allCorrect

            question.steps.forEach { step ->
                val right = response.correctness[step.variant.person] == true
                if (right) correct++ else incorrect++
                results += WordResultEntry(
                    word = step.spokenForm,
                    translation = "${question.infinitive} · ${step.variant.person.label}",
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

            val question = nextQuestion()
            if (question == null) {
                lastSessionResultsHolder.wordResults = results.toList()
                _navigationEvents.emit(SessionNavigationEvent.SessionComplete(correct, incorrect, 0, 0))
                return@launch
            }

            _uiState.update {
                it.copy(
                    question = question,
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
        val question = _uiState.value.question ?: return
        _uiState.update { it.copy(isPickingImage = true) }
        viewModelScope.launch(dispatchers.io) {
            val choices = loadImageChoices(question.infinitive, question.translation)
            _uiState.update { it.copy(imageChoices = choices) }
        }
    }

    fun onImageChosen(url: String) {
        val question = _uiState.value.question ?: return
        _uiState.update {
            it.copy(isPickingImage = false, imageChoices = persistentListOf(), question = it.question?.copy(imageUrl = url))
        }
        viewModelScope.launch(dispatchers.io) { chooseImage(question.infinitive, question.translation, url) }
    }

    fun onImagePickerDismissed() = _uiState.update { it.copy(isPickingImage = false, imageChoices = persistentListOf()) }
}
