package com.lexicon.presentation.vocabularycourse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.interactors.vocabularycourse.GetVocabularyCourseUseCase
import com.lexicon.interactors.vocabularycourse.GetWordCardsUseCase
import com.lexicon.interactors.vocabularycourse.MarkCourseCardsSeenUseCase
import com.lexicon.interactors.vocabularycourse.NextCourseTrainingUseCase
import com.lexicon.interactors.vocabularycourse.WordCard
import com.lexicon.model.vocabulary.ExampleSentence
import com.lexicon.presentation.dashboard.LaunchTraining
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WordCardsUiState(
    val isLoading: Boolean = true,
    val cards: ImmutableList<WordCard> = persistentListOf(),
    val index: Int = 0,
    val isFinished: Boolean = false,
    val launch: LaunchTraining? = null,
) {
    val current: WordCard? get() = cards.getOrNull(index)

    val isLast: Boolean get() = index >= cards.lastIndex
}

class WordCardsViewModel(
    private val getCourse: GetVocabularyCourseUseCase,
    private val getCards: GetWordCardsUseCase,
    private val markSeen: MarkCourseCardsSeenUseCase,
    private val queue: NextCourseTrainingUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WordCardsUiState())
    val uiState: StateFlow<WordCardsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val cards = getCards(getCourse().newWords)
            _uiState.update { it.copy(isLoading = false, cards = cards, index = it.index.coerceAtMost(cards.lastIndex.coerceAtLeast(0))) }
        }
    }

    fun onNext() {
        val state = _uiState.value
        if (!state.isLast) {
            _uiState.update { it.copy(index = it.index + 1) }
            return
        }
        viewModelScope.launch {
            markSeen()
            val next = queue.next()
            _uiState.update {
                it.copy(
                    isFinished = true,
                    launch = next?.let { launch -> LaunchTraining(launch.training, launch.wordIds) },
                )
            }
        }
    }

    fun onPrevious() = _uiState.update { it.copy(index = (it.index - 1).coerceAtLeast(0)) }

    fun onSpeakExample() {
        val sentence = ExampleSentence.parse(_uiState.value.current?.example.orEmpty())
        if (sentence.isBlank) return
        viewModelScope.launch { runCatching { speechSynthesizer.speak(sentence.text) } }
    }

    fun onPronounce() {
        val word = _uiState.value.current ?: return
        viewModelScope.launch { runCatching { speechSynthesizer.speak(word.text) } }
    }
}
