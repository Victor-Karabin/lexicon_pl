package com.lexicon.presentation.numbers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.numbers.NextNumberUseCase
import com.lexicon.interactors.numbers.NumberForm
import com.lexicon.interactors.numbers.NumberQuestion
import com.lexicon.interactors.numbers.SubmitNumberAnswerUseCase
import com.lexicon.interactors.settings.AppSettings
import com.lexicon.interactors.training.RecordStudyActivityUseCase
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.WordResultEntry
import com.lexicon.presentation.common.markIn
import com.lexicon.presentation.common.speakQuietly
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NumberTrainingUiState(
    val isLoading: Boolean = true,
    val question: NumberQuestion? = null,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val answer: String = "",
    val verdict: AnswerVerdict? = null,
) {
    val isAnswered: Boolean get() = verdict != null

    val canCheck: Boolean get() = !isAnswered && question != null && answer.isNotBlank()
}

class NumberTrainingViewModel(
    private val nextNumber: NextNumberUseCase,
    private val submitAnswer: SubmitNumberAnswerUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val lastSessionResultsHolder: LastSessionResultsHolder,
    private val recordStudy: RecordStudyActivityUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NumberTrainingUiState())
    val uiState: StateFlow<NumberTrainingUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<SessionNavigationEvent>()
    val navigationEvents: SharedFlow<SessionNavigationEvent> = _navigationEvents.asSharedFlow()

    private val asked = mutableSetOf<NumberForm>()
    private val results = mutableListOf<WordResultEntry>()
    private var correct = 0
    private var incorrect = 0

    init {
        viewModelScope.launch(dispatchers.io) {
            val first = nextNumber(asked)?.also { asked += it.form }
            _uiState.update { it.copy(isLoading = false, question = first, totalSteps = AppSettings.DEFAULT_STEP_COUNT) }
        }
    }

    fun onAnswerChanged(value: String) {
        studied()
        _uiState.update { if (it.isAnswered) it else it.copy(answer = value) }
    }

    fun onCheck() {
        val state = _uiState.value
        val question = state.question ?: return
        if (!state.canCheck) return
        studied()

        viewModelScope.launch(dispatchers.io) {
            val verdict = submitAnswer(question, state.answer)
            val right = verdict == AnswerVerdict.CORRECT
            if (right) correct++ else incorrect++
            results += WordResultEntry(
                word = question.words,
                translation = question.digits,
                outcome = if (right) AnswerState.Correct else AnswerState.Incorrect(question.words),
                typed = state.answer,
            )
            _uiState.update { it.copy(verdict = verdict) }
        }
    }

    fun onNext() {
        studied()
        viewModelScope.launch(dispatchers.io) {
            val nextIndex = _uiState.value.stepIndex + 1
            val question = if (nextIndex < _uiState.value.totalSteps) nextNumber(asked) else null

            if (question == null) {
                lastSessionResultsHolder.wordResults = results.toList()
                _navigationEvents.emit(SessionNavigationEvent.SessionComplete(correct, incorrect, 0, 0))
                return@launch
            }

            asked += question.form
            _uiState.update { it.copy(question = question, stepIndex = nextIndex, answer = "", verdict = null) }
        }
    }

    fun onSpeak() {
        val words = _uiState.value.question?.words ?: return
        viewModelScope.launch(dispatchers.io) { speechSynthesizer.speakQuietly(words) }
    }

    private fun studied() = recordStudy.markIn(viewModelScope, dispatchers)
}
