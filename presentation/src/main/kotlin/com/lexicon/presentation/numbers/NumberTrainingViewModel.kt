package com.lexicon.presentation.numbers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.numbers.NextNumberUseCase
import com.lexicon.interactors.numbers.NumberQuestion
import com.lexicon.interactors.numbers.SubmitNumberAnswerUseCase
import com.lexicon.interactors.settings.GetStepCountUseCase
import com.lexicon.interactors.training.RecordStudyActivityUseCase
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.StepSession
import com.lexicon.presentation.common.WordResultEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
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
    nextNumber: NextNumberUseCase,
    private val submitAnswer: SubmitNumberAnswerUseCase,
    speechSynthesizer: SpeechSynthesizer,
    lastSessionResultsHolder: LastSessionResultsHolder,
    recordStudy: RecordStudyActivityUseCase,
    getStepCount: GetStepCountUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NumberTrainingUiState())
    val uiState: StateFlow<NumberTrainingUiState> = _uiState.asStateFlow()

    private val session = StepSession(
        scope = viewModelScope,
        dispatchers = dispatchers,
        speechSynthesizer = speechSynthesizer,
        resultsHolder = lastSessionResultsHolder,
        recordStudy = recordStudy,
        getStepCount = getStepCount,
        keyOf = NumberQuestion::form,
        pick = { asked -> nextNumber(asked) },
    )

    val navigationEvents: SharedFlow<SessionNavigationEvent> = session.navigationEvents

    init {
        session.begin { first ->
            _uiState.update { it.copy(isLoading = false, question = first, totalSteps = session.totalSteps) }
        }
    }

    fun onAnswerChanged(value: String) {
        session.studied()
        _uiState.update { if (it.isAnswered) it else it.copy(answer = value) }
    }

    fun onCheck() {
        val state = _uiState.value
        val question = state.question ?: return
        if (!state.canCheck) return
        session.studied()

        viewModelScope.launch(dispatchers.io) {
            val verdict = submitAnswer(question, state.answer)
            session.record(
                WordResultEntry(
                    word = question.words,
                    translation = question.digits,
                    translationText = question.label(),
                    outcome = if (verdict == AnswerVerdict.CORRECT) AnswerState.Correct else AnswerState.Incorrect(question.words),
                    typed = state.answer,
                ),
            )
            _uiState.update { it.copy(verdict = verdict) }
        }
    }

    fun onNext() {
        if (!_uiState.value.isAnswered) return
        session.studied()
        session.advance(_uiState.value.stepIndex) { question, index ->
            _uiState.update { it.copy(question = question, stepIndex = index, answer = "", verdict = null) }
        }
    }

    fun onSpeak() {
        val words = _uiState.value.question?.words ?: return
        session.speak(words)
    }
}
