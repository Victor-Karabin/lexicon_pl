package com.lexicon.presentation.conjugation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.android.speech.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.conjugation.ChooseVerbImageUseCase
import com.lexicon.interactors.conjugation.ConjugationCourseProgress
import com.lexicon.interactors.conjugation.ConjugationQuestion
import com.lexicon.interactors.conjugation.LoadConjugationProgressUseCase
import com.lexicon.interactors.conjugation.LoadVerbImageChoicesUseCase
import com.lexicon.interactors.conjugation.NextConjugationQuestionUseCase
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerRequest
import com.lexicon.interactors.conjugation.SubmitConjugationAnswerUseCase
import com.lexicon.presentation.common.AnswerState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConjugationUiState(
    val isLoading: Boolean = true,
    val question: ConjugationQuestion? = null,
    val selected: String? = null,
    val answerState: AnswerState = AnswerState.Unanswered,
    val progress: ConjugationCourseProgress = ConjugationCourseProgress(persistentListOf()),
    val hasNoVerbs: Boolean = false,
    val isPickingImage: Boolean = false,
    val imageChoices: ImmutableList<String> = persistentListOf(),
    val isCourseComplete: Boolean = false,
) {
    val options: ImmutableList<String> get() = question?.options ?: persistentListOf()

    val isAnswered: Boolean get() = answerState !is AnswerState.Unanswered

    val canCheck: Boolean get() = selected != null && !isAnswered
}

class ConjugationViewModel(
    private val nextQuestion: NextConjugationQuestionUseCase,
    private val submitAnswer: SubmitConjugationAnswerUseCase,
    private val loadProgress: LoadConjugationProgressUseCase,
    private val loadImageChoices: LoadVerbImageChoicesUseCase,
    private val chooseImage: ChooseVerbImageUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConjugationUiState())
    val uiState: StateFlow<ConjugationUiState> = _uiState.asStateFlow()

    init {
        advance()
    }

    fun onOptionSelected(option: String) {
        if (_uiState.value.isAnswered) return
        _uiState.update { it.copy(selected = option) }
    }

    fun onCheck() {
        val state = _uiState.value
        val question = state.question ?: return
        if (!state.canCheck) return

        viewModelScope.launch(dispatchers.io) {
            val response = submitAnswer(SubmitConjugationAnswerRequest(question, state.selected))
            _uiState.update {
                it.copy(
                    answerState = if (response.isCorrect) {
                        AnswerState.Correct
                    } else {
                        AnswerState.Incorrect(response.correctOptions.firstOrNull().orEmpty())
                    },
                    progress = loadProgress(),
                )
            }
        }
    }

    fun onNext() = advance()

    fun onEditImage() {
        val infinitive = _uiState.value.question?.variant?.infinitive ?: return
        _uiState.update { it.copy(isPickingImage = true) }
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(imageChoices = loadImageChoices(infinitive)) }
        }
    }

    fun onImageChosen(url: String) {
        val question = _uiState.value.question ?: return
        viewModelScope.launch(dispatchers.io) {
            chooseImage(question.variant.infinitive, url)
            _uiState.update {
                it.copy(
                    isPickingImage = false,
                    question = it.question?.copy(imageUrl = url),
                    imageChoices = persistentListOf(),
                )
            }
        }
    }

    fun onImagePickerDismissed() = _uiState.update { it.copy(isPickingImage = false, imageChoices = persistentListOf()) }

    fun onSpeak() {
        val spoken = _uiState.value.question?.spokenForm.orEmpty()
        if (spoken.isBlank()) return
        viewModelScope.launch(dispatchers.io) { runCatching { speechSynthesizer.speak(spoken) } }
    }

    private fun advance() {
        viewModelScope.launch(dispatchers.io) {
            val progress = loadProgress()
            val question = nextQuestion()
            _uiState.update {
                ConjugationUiState(
                    isLoading = false,
                    question = question,
                    progress = progress,
                    hasNoVerbs = progress.total == 0,
                    isCourseComplete = progress.total > 0 && progress.isComplete,
                )
            }
        }
    }
}
