package com.lexicon.presentation.program

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.boundary.SpeechSynthesizer
import com.lexicon.interactors.program.GetProgramDayUseCase
import com.lexicon.interactors.program.GetWordCardsUseCase
import com.lexicon.interactors.program.MarkCardsSeenUseCase
import com.lexicon.interactors.program.ProgramId
import com.lexicon.interactors.program.WordCard
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
    savedStateHandle: SavedStateHandle,
    private val getDay: GetProgramDayUseCase,
    private val getCards: GetWordCardsUseCase,
    private val markSeen: MarkCardsSeenUseCase,
    private val queue: ProgramQueue,
    private val speechSynthesizer: SpeechSynthesizer,
) : ViewModel() {
    private val programId = ProgramId(savedStateHandle.get<String>(PROGRAM_ID_ARG).orEmpty())

    private val _uiState = MutableStateFlow(WordCardsUiState())
    val uiState: StateFlow<WordCardsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val day = getDay(programId)
            val cards = day?.newWords?.let { getCards(it) } ?: persistentListOf()
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
            markSeen(programId)
            val next = queue.next(programId)
            _uiState.update {
                it.copy(
                    isFinished = true,
                    launch = next?.let { LaunchTraining(it.training, it.wordIds) },
                )
            }
        }
    }

    fun onPrevious() = _uiState.update { it.copy(index = (it.index - 1).coerceAtLeast(0)) }

    fun onPronounce() {
        val word = _uiState.value.current ?: return
        viewModelScope.launch { runCatching { speechSynthesizer.speak(word.text) } }
    }
}
