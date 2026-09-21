package com.lexicon.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.presets.CountWordsToReviewUseCase
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.GetWordUseCase
import com.lexicon.interactors.presets.GetWordsToReviewUseCase
import com.lexicon.interactors.presets.SetWordStatusUseCase
import com.lexicon.interactors.vocabularycourse.GetWordCardsUseCase
import com.lexicon.model.vocabulary.ExampleSentence
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SETTLE_MS = 350L

private const val PICTURES_AHEAD = 2

data class ReviewWordsUiState(
    val isLoading: Boolean = true,
    val words: ImmutableList<Word> = persistentListOf(),
    val index: Int = 0,
    val reviewed: Int = 0,
    val waiting: Int = 0,
    val chosen: WordStatus? = null,
    val pictures: ImmutableMap<VocabularyId, String> = persistentMapOf(),
) {
    val current: Word? get() = words.getOrNull(index)

    val currentPicture: String? get() = current?.let { pictures[it.id] }

    val isFinished: Boolean get() = !isLoading && current == null
}

class ReviewWordsViewModel(
    private val getWordsToReview: GetWordsToReviewUseCase,
    private val countWordsToReview: CountWordsToReviewUseCase,
    private val setWordStatus: SetWordStatusUseCase,
    private val deleteWord: DeleteWordUseCase,
    private val getWord: GetWordUseCase,
    private val getWordCards: GetWordCardsUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewWordsUiState())
    val uiState: StateFlow<ReviewWordsUiState> = _uiState.asStateFlow()

    private var settleJob: Job? = null
    private val picturesAsked = mutableSetOf<VocabularyId>()

    init {
        viewModelScope.launch(dispatchers.io) {
            val waiting = countWordsToReview()
            val words = getWordsToReview()
            _uiState.update { it.copy(isLoading = false, words = words, waiting = waiting) }
            loadPictures()
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

    fun refreshCurrent() {
        val id = _uiState.value.current?.id ?: return

        viewModelScope.launch(dispatchers.io) {
            val word = getWord(id) ?: return@launch
            val picture = getWordCards(listOf(id)).firstOrNull()?.imageUrl
            _uiState.update { state ->
                state.copy(
                    words = state.words.map { if (it.id == id) word else it }.toImmutableList(),
                    pictures = state.pictures.toPersistentMap().let { pictures ->
                        picture?.let { pictures.put(id, it) } ?: pictures.remove(id)
                    },
                )
            }
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
        if (state.index >= state.words.size) {
            val next = getWordsToReview()
            _uiState.update { it.copy(words = next.toImmutableList(), index = 0) }
        }
        loadPictures()
    }

    private fun loadPictures() {
        val state = _uiState.value
        val wanted = state.words
            .drop(state.index)
            .take(PICTURES_AHEAD)
            .map { it.id }
            .filter { picturesAsked.add(it) }
        if (wanted.isEmpty()) return

        viewModelScope.launch(dispatchers.io) {
            val found = getWordCards(wanted).mapNotNull { card -> card.imageUrl?.let { card.id to it } }
            if (found.isEmpty()) return@launch
            _uiState.update { it.copy(pictures = it.pictures.toPersistentMap().putAll(found.toMap())) }
        }
    }
}
