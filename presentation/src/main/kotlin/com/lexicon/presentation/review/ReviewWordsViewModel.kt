package com.lexicon.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.CountWordsToReviewUseCase
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.GetWordsToReviewUseCase
import com.lexicon.interactors.presets.SetWordStatusUseCase
import com.lexicon.model.vocabulary.ExampleSentence
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SETTLE_MS = 350L

data class ReviewWordsUiState(
    val isLoading: Boolean = true,
    val words: ImmutableList<Word> = persistentListOf(),
    val index: Int = 0,
    val reviewed: Int = 0,
    val waiting: Int = 0,
    val chosen: WordStatus? = null,
) {
    val current: Word? get() = words.getOrNull(index)

    val isFinished: Boolean get() = !isLoading && current == null
}

class ReviewWordsViewModel(
    private val getWordsToReview: GetWordsToReviewUseCase,
    private val countWordsToReview: CountWordsToReviewUseCase,
    private val setWordStatus: SetWordStatusUseCase,
    private val deleteWord: DeleteWordUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewWordsUiState())
    val uiState: StateFlow<ReviewWordsUiState> = _uiState.asStateFlow()

    private var settleJob: Job? = null

    init {
        viewModelScope.launch(dispatchers.io) {
            val waiting = countWordsToReview()
            val words = getWordsToReview()
            _uiState.update { it.copy(isLoading = false, words = words, waiting = waiting) }
        }
    }

    fun onStatusChosen(status: WordStatus) {
        val word = _uiState.value.current ?: return
        _uiState.update { it.copy(chosen = status) }

        settleJob?.cancel()
        settleJob = viewModelScope.launch(dispatchers.io) {
            delay(SETTLE_MS)
            setWordStatus(word.id, _uiState.value.chosen ?: status)
            advance()
        }
    }

    fun onDeleted() {
        val word = _uiState.value.current ?: return
        _uiState.update { it.copy(chosen = null) }

        settleJob?.cancel()
        settleJob = viewModelScope.launch(dispatchers.io) {
            delay(SETTLE_MS)
            deleteWord(word.id)
            advance()
        }
    }

    fun onPronounce() {
        val word = _uiState.value.current ?: return
        viewModelScope.launch(dispatchers.io) { runCatching { speechSynthesizer.speak(word.text) } }
    }

    fun onSpeakExample() {
        val word = _uiState.value.current ?: return
        val sentence = ExampleSentence.of(word.example, word = word.text)
        if (sentence.isBlank) return

        viewModelScope.launch(dispatchers.io) { runCatching { speechSynthesizer.speak(sentence.text) } }
    }

    private suspend fun advance() {
        _uiState.update {
            it.copy(
                index = it.index + 1,
                reviewed = it.reviewed + 1,
                waiting = (it.waiting - 1).coerceAtLeast(0),
                chosen = null,
            )
        }

        val state = _uiState.value
        if (state.index < state.words.size) return

        val next = getWordsToReview()
        _uiState.update { it.copy(words = next.toImmutableList(), index = 0) }
    }
}
