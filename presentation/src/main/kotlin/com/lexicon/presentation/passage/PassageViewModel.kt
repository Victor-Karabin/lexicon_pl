package com.lexicon.presentation.passage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.android.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.passage.Passage
import com.lexicon.interactors.passage.StartPassageSessionRequest
import com.lexicon.interactors.passage.StartPassageSessionUseCase
import com.lexicon.interactors.passage.SubmitPassageAnswersRequest
import com.lexicon.interactors.passage.SubmitPassageAnswersUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PassageUiState(
    val isLoading: Boolean = true,
    val passage: Passage? = null,
    val bank: ImmutableList<String> = persistentListOf(),
    val answers: ImmutableList<String> = persistentListOf(),
    val correctness: ImmutableList<Boolean> = persistentListOf(),
    val isChecked: Boolean = false,
    val isSpeaking: Boolean = false,
) {
    val expected: List<String> get() = passage?.gaps?.map { it.answer }.orEmpty()

    val correctCount: Int get() = correctness.count { it }

    val usedBankWords: Set<String> get() = answers.filter { it.isNotBlank() }.toSet()
}

const val PASSAGE_BANK_ARG = "bank"

class PassageViewModel(
    savedStateHandle: SavedStateHandle,
    private val startSession: StartPassageSessionUseCase,
    private val submitAnswers: SubmitPassageAnswersUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val withWordBank: Boolean = savedStateHandle.get<String>(PASSAGE_BANK_ARG).toBoolean()

    private val _uiState = MutableStateFlow(PassageUiState())
    val uiState: StateFlow<PassageUiState> = _uiState.asStateFlow()

    private var sessionId: String = ""

    init {
        viewModelScope.launch(dispatchers.io) {
            val session = startSession(StartPassageSessionRequest(withWordBank = withWordBank))
            if (session == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            sessionId = session.sessionId
            _uiState.update {
                it.copy(
                    isLoading = false,
                    passage = session.passage,
                    bank = session.bank,
                    answers = List(session.passage.gaps.size) { "" }.toImmutableList(),
                )
            }
        }
    }

    fun onSpeak() {
        val text = _uiState.value.passage?.plainText ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSpeaking = true) }
            runCatching { speechSynthesizer.speak(text) }
            _uiState.update { it.copy(isSpeaking = false) }
        }
    }

    fun onAnswerChanged(
        index: Int,
        value: String,
    ) = _uiState.update { state ->
        val answers = state.answers.toMutableList()
        if (index !in answers.indices) return@update state
        answers[index] = value
        state.copy(answers = answers.toImmutableList())
    }

    fun onBankWordSelected(word: String) =
        _uiState.update { state ->
            val at = state.answers.indexOfFirst { it.isBlank() }
            if (at < 0) return@update state
            val answers = state.answers.toMutableList()
            answers[at] = word
            state.copy(answers = answers.toImmutableList())
        }

    fun onGapCleared(index: Int) = onAnswerChanged(index, "")

    fun onCheck(onComplete: (Int, Int, Int, Int) -> Unit) {
        val state = _uiState.value
        val passage = state.passage ?: return
        viewModelScope.launch(dispatchers.io) {
            val result = submitAnswers(
                SubmitPassageAnswersRequest(
                    sessionId = sessionId,
                    passageId = passage.id,
                    expected = state.expected,
                    answers = state.answers,
                ),
            )
            val correct = result.correct.count { it }
            _uiState.update {
                it.copy(isChecked = true, correctness = result.correct.toImmutableList())
            }
            onComplete(correct, result.correct.size - correct, 0, 0)
        }
    }
}
