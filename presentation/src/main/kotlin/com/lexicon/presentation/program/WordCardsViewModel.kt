package com.lexicon.presentation.program

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.android.SpeechSynthesizer
import com.lexicon.interactors.program.GetProgramDayUseCase
import com.lexicon.interactors.program.GetWordCardsUseCase
import com.lexicon.interactors.program.MarkCardsSeenUseCase
import com.lexicon.interactors.program.ProgramId
import com.lexicon.interactors.program.StartProgramSessionUseCase
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
    /** Set once the deck is through; the screen leaves on it. */
    val isFinished: Boolean = false,
    /** Where the deck leads: the first training of the day, over the day's words. */
    val launch: LaunchTraining? = null,
) {
    val current: WordCard? get() = cards.getOrNull(index)

    val isLast: Boolean get() = index >= cards.lastIndex
}

/**
 * The new words of the day, one card at a time, before any of them are drilled.
 *
 * Meeting a word and being tested on it are different things, and a training that
 * asks about a word never seen is a guess. The deck is marked as seen only on
 * reaching the end, so backing out halfway shows the rest next time.
 */
class WordCardsViewModel(
    savedStateHandle: SavedStateHandle,
    private val getDay: GetProgramDayUseCase,
    private val getCards: GetWordCardsUseCase,
    private val markSeen: MarkCardsSeenUseCase,
    private val startSession: StartProgramSessionUseCase,
    private val speechSynthesizer: SpeechSynthesizer,
) : ViewModel() {
    private val programId = ProgramId(savedStateHandle.get<String>(PROGRAM_ID_ARG).orEmpty())

    private val _uiState = MutableStateFlow(WordCardsUiState())
    val uiState: StateFlow<WordCardsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Reloaded after an edit, so a corrected word shows as corrected. */
    fun load() {
        viewModelScope.launch {
            val day = getDay(programId)
            val cards = day?.newWords?.let { getCards(it) } ?: persistentListOf()
            _uiState.update { it.copy(isLoading = false, cards = cards, index = it.index.coerceAtMost(cards.lastIndex.coerceAtLeast(0))) }
        }
    }

    /**
     * The last card leads straight into the first training of the day.
     *
     * Meeting ten words and then being put back on the Dashboard to press Continue
     * is a step that asks nothing of the learner, so the deck hands over directly.
     */
    fun onNext() {
        val state = _uiState.value
        if (!state.isLast) {
            _uiState.update { it.copy(index = it.index + 1) }
            return
        }
        viewModelScope.launch {
            markSeen(programId)
            val next = getDay(programId)?.nextTraining
            val session = next?.let { startSession(programId) }
            _uiState.update {
                it.copy(
                    isFinished = true,
                    launch = next?.let { queued ->
                        LaunchTraining(queued.training, session?.wordIds ?: persistentListOf())
                    },
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
