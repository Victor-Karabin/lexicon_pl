package com.lexicon.presentation.passage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.passage.Passage
import com.lexicon.interactors.passage.PassageSegment
import com.lexicon.interactors.passage.PassageSessionResult
import com.lexicon.interactors.passage.StartPassageSessionRequest
import com.lexicon.interactors.passage.StartPassageSessionUseCase
import com.lexicon.interactors.passage.SubmitPassageAnswersRequest
import com.lexicon.interactors.passage.SubmitPassageAnswersUseCase
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.WordResultEntry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PassageProblem { NONE, EMPTY_STUDY_SET, OFFLINE, REFUSED }

private const val GAP_PAUSE_MS = 2_500L

private const val SENTENCE_PAUSE_MS = 700L

data class PassageUiState(
    val isLoading: Boolean = true,
    val problem: PassageProblem = PassageProblem.NONE,
    val passage: Passage? = null,
    val bank: ImmutableList<String> = persistentListOf(),
    val answers: ImmutableList<String> = persistentListOf(),
    val correctness: ImmutableList<Boolean> = persistentListOf(),
    val isChecked: Boolean = false,
    val isSpeaking: Boolean = false,
) {
    val expected: List<String> get() = passage?.gaps?.map { it.answer }.orEmpty()

    /** The starred word behind each gap, in gap order. */
    val words: List<String> get() = passage?.gaps?.map { it.word }.orEmpty()

    val correctCount: Int get() = correctness.count { it }

    val usedBankWords: Set<String> get() = answers.filter { it.isNotBlank() }.toSet()
}

const val PASSAGE_BANK_ARG = "bank"

class PassageViewModel(
    savedStateHandle: SavedStateHandle,
    private val startSession: StartPassageSessionUseCase,
    private val submitAnswers: SubmitPassageAnswersUseCase,
    private val lastSessionResultsHolder: LastSessionResultsHolder,
    private val speechSynthesizer: SpeechSynthesizer,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val withWordBank: Boolean = savedStateHandle.get<String>(PASSAGE_BANK_ARG).toBoolean()

    private val _uiState = MutableStateFlow(PassageUiState())
    val uiState: StateFlow<PassageUiState> = _uiState.asStateFlow()

    private var sessionId: String = ""

    init {
        viewModelScope.launch(dispatchers.io) {
            when (val session = startSession(StartPassageSessionRequest(withWordBank = withWordBank))) {
                is PassageSessionResult.Ready -> {
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

                PassageSessionResult.EmptyStudySet ->
                    _uiState.update { it.copy(isLoading = false, problem = PassageProblem.EMPTY_STUDY_SET) }

                PassageSessionResult.Offline ->
                    _uiState.update { it.copy(isLoading = false, problem = PassageProblem.OFFLINE) }

                is PassageSessionResult.Refused ->
                    _uiState.update { it.copy(isLoading = false, problem = PassageProblem.REFUSED) }
            }
        }
    }

    fun onSpeak() {
        val passage = _uiState.value.passage ?: return
        if (_uiState.value.isSpeaking) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSpeaking = true) }
            for (sentence in passage.sentences) {
                for (segment in sentence.segments) {
                    val spoken = when (segment) {
                        is PassageSegment.Text -> segment.text
                        is PassageSegment.Gap -> segment.answer
                    }
                    if (spoken.isNotBlank()) runCatching { speechSynthesizer.speak(spoken) }
                    if (segment is PassageSegment.Gap) delay(GAP_PAUSE_MS)
                }
                delay(SENTENCE_PAUSE_MS)
            }
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

    fun onCheck() {
        val state = _uiState.value
        state.passage ?: return
        viewModelScope.launch(dispatchers.io) {
            val result = submitAnswers(
                SubmitPassageAnswersRequest(
                    sessionId = sessionId,
                    expected = state.expected,
                    answers = state.answers,
                    words = state.words,
                ),
            )

            // The same word-by-word breakdown every other training hands the result
            // screen, so a single-screen training reads no differently at the end.
            lastSessionResultsHolder.wordResults = result.results.map { gap ->
                WordResultEntry(
                    word = gap.expected,
                    translation = gap.translation,
                    outcome = if (gap.isCorrect) AnswerState.Correct else AnswerState.Incorrect(gap.expected),
                )
            }

            _uiState.update {
                it.copy(isChecked = true, correctness = result.correct.toImmutableList())
            }
        }
    }
}
