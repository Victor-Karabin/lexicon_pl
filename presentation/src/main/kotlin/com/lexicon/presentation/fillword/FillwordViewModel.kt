package com.lexicon.presentation.fillword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.fillword.FillwordCell
import com.lexicon.interactors.fillword.FillwordPuzzle
import com.lexicon.interactors.fillword.FillwordSessionResult
import com.lexicon.interactors.fillword.FillwordWord
import com.lexicon.interactors.fillword.StartFillwordSessionUseCase
import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.LastSessionResultsHolder
import com.lexicon.presentation.common.WordResultEntry
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FillwordUiState(
    val isLoading: Boolean = true,
    val puzzle: FillwordPuzzle? = null,
    val found: ImmutableSet<String> = persistentSetOf(),
    val foundCells: ImmutableSet<FillwordCell> = persistentSetOf(),
    val anchor: FillwordCell? = null,
    val isRevealed: Boolean = false,
) {
    val total: Int get() = puzzle?.words?.size ?: 0

    val isComplete: Boolean get() = total > 0 && found.size == total

    val missing: List<FillwordWord> get() = puzzle?.words.orEmpty().filterNot { it.word in found }

    val missingCells: Set<FillwordCell>
        get() = if (!isRevealed) emptySet() else missing.flatMap { it.cells }.toSet() - foundCells

    val isFinished: Boolean get() = isComplete || isRevealed
}

class FillwordViewModel(
    private val startSession: StartFillwordSessionUseCase,
    private val lastSessionResultsHolder: LastSessionResultsHolder,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FillwordUiState())
    val uiState: StateFlow<FillwordUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            when (val session = startSession()) {
                is FillwordSessionResult.Ready ->
                    _uiState.update { it.copy(isLoading = false, puzzle = session.puzzle) }

                FillwordSessionResult.EmptyStudySet -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onCheck() = _uiState.update { it.copy(isRevealed = true, anchor = null) }

    fun onFinished() {
        val puzzle = _uiState.value.puzzle ?: return
        val found = _uiState.value.found

        lastSessionResultsHolder.wordResults = puzzle.words.map { word ->
            WordResultEntry(
                word = word.word,
                translation = puzzle.translationOf(word),
                outcome = if (word.word in found) AnswerState.Correct else AnswerState.Incorrect(word.word),
            )
        }
    }

    fun onCellTapped(cell: FillwordCell) =
        _uiState.update { state ->
            if (state.isRevealed) return@update state
            val anchor = state.anchor
            when {
                anchor == null -> state.copy(anchor = cell)
                anchor == cell -> state.copy(anchor = null)
                else -> state.claim(anchor, cell)
            }
        }

    fun onCellsTraced(
        from: FillwordCell,
        to: FillwordCell,
    ) = _uiState.update { it.claim(from, to) }

    private fun FillwordUiState.claim(
        from: FillwordCell,
        to: FillwordCell,
    ): FillwordUiState {
        if (isRevealed) return this
        val puzzle = puzzle ?: return this
        val word = puzzle.wordAlong(from, to) ?: return copy(anchor = null)
        return copy(
            anchor = null,
            found = (found + word.word).toImmutableSet(),
            foundCells = (foundCells + puzzle.runBetween(from, to)).toImmutableSet(),
        )
    }
}
