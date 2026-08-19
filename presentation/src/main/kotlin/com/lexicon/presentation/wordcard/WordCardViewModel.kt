package com.lexicon.presentation.wordcard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.wordcard.RecordWordCardSeenRequest
import com.lexicon.interactors.wordcard.RecordWordCardSeenUseCase
import com.lexicon.interactors.wordcard.StartWordCardSessionRequest
import com.lexicon.interactors.wordcard.StartWordCardSessionUseCase
import com.lexicon.interactors.wordcard.WordCardStep
import com.lexicon.presentation.common.trainingVocabularyIds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WordCardUiState(
    val isLoading: Boolean = true,
    val cards: ImmutableList<WordCardStep> = persistentListOf(),
    val index: Int = 0,
    val isFinished: Boolean = false,
) {
    val current: WordCardStep? get() = cards.getOrNull(index)

    val isLast: Boolean get() = index >= cards.lastIndex
}

class WordCardViewModel(
    savedStateHandle: SavedStateHandle,
    private val startSession: StartWordCardSessionUseCase,
    private val recordSeen: RecordWordCardSeenUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val vocabularyIds = savedStateHandle.trainingVocabularyIds()

    private val _uiState = MutableStateFlow(WordCardUiState())
    val uiState: StateFlow<WordCardUiState> = _uiState.asStateFlow()

    private var sessionId: String = ""

    private val recorded = mutableSetOf<Int>()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch(dispatchers.io) {
            val session = startSession(StartWordCardSessionRequest(vocabularyIds = vocabularyIds))
            sessionId = session.sessionId
            _uiState.update {
                it.copy(
                    isLoading = false,
                    cards = session.steps.toImmutableList(),
                    index = it.index.coerceAtMost(session.steps.lastIndex.coerceAtLeast(0)),
                )
            }
            recordCurrent()
        }
    }

    fun onNext() {
        val state = _uiState.value
        if (state.isLast) {
            _uiState.update { it.copy(isFinished = true) }
            return
        }
        _uiState.update { it.copy(index = it.index + 1) }
        recordCurrent()
    }

    fun onPrevious() = _uiState.update { it.copy(index = (it.index - 1).coerceAtLeast(0)) }

    fun onPronounce() {
        val card = _uiState.value.current ?: return
        viewModelScope.launch { runCatching { speechSynthesizer.speak(card.text) } }
    }

    private fun recordCurrent() {
        val state = _uiState.value
        val card = state.current ?: return
        if (!recorded.add(state.index)) return
        viewModelScope.launch(dispatchers.io) {
            recordSeen(
                RecordWordCardSeenRequest(
                    sessionId = sessionId,
                    stepIndex = card.stepIndex,
                    vocabularyItemId = card.vocabularyItemId,
                    text = card.text,
                ),
            )
        }
    }
}
