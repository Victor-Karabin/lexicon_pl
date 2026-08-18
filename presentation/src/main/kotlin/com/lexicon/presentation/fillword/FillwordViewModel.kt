package com.lexicon.presentation.fillword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.fillword.FillwordCell
import com.lexicon.interactors.fillword.FillwordPuzzle
import com.lexicon.interactors.fillword.FillwordSessionResult
import com.lexicon.interactors.fillword.StartFillwordSessionUseCase
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FillwordProblem { NONE, NO_FAVOURITES, OFFLINE, REFUSED }

data class FillwordUiState(
    val isLoading: Boolean = true,
    val problem: FillwordProblem = FillwordProblem.NONE,
    val puzzle: FillwordPuzzle? = null,
    val found: ImmutableSet<String> = persistentSetOf(),
    /**
     * The cells of the runs already claimed.
     *
     * Held rather than derived: the same letters can spell a word in more than one
     * place, and the run the learner actually traced is the one to light up.
     */
    val foundCells: ImmutableSet<FillwordCell> = persistentSetOf(),
    /** The first corner of a pair, waiting for the second. */
    val anchor: FillwordCell? = null,
) {
    val total: Int get() = puzzle?.words?.size ?: 0

    val isComplete: Boolean get() = total > 0 && found.size == total
}

class FillwordViewModel(
    private val startSession: StartFillwordSessionUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FillwordUiState())
    val uiState: StateFlow<FillwordUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            when (val session = startSession()) {
                is FillwordSessionResult.Ready ->
                    _uiState.update { it.copy(isLoading = false, puzzle = session.puzzle) }

                FillwordSessionResult.NoFavourites ->
                    _uiState.update { it.copy(isLoading = false, problem = FillwordProblem.NO_FAVOURITES) }

                FillwordSessionResult.Offline ->
                    _uiState.update { it.copy(isLoading = false, problem = FillwordProblem.OFFLINE) }

                is FillwordSessionResult.Refused ->
                    _uiState.update { it.copy(isLoading = false, problem = FillwordProblem.REFUSED) }
            }
        }
    }

    /**
     * A word can also be claimed by tapping its two ends, for anyone who finds dragging
     * across small cells fiddly. The first tap parks an anchor, the second completes it.
     */
    fun onCellTapped(cell: FillwordCell) =
        _uiState.update { state ->
            val anchor = state.anchor
            when {
                anchor == null -> state.copy(anchor = cell)
                anchor == cell -> state.copy(anchor = null)
                else -> state.claim(anchor, cell)
            }
        }

    /** A run traced with a finger, from wherever it started to wherever it was lifted. */
    fun onCellsTraced(
        from: FillwordCell,
        to: FillwordCell,
    ) = _uiState.update { it.claim(from, to) }

    private fun FillwordUiState.claim(
        from: FillwordCell,
        to: FillwordCell,
    ): FillwordUiState {
        val puzzle = puzzle ?: return this
        val word = puzzle.wordAlong(from, to) ?: return copy(anchor = null)
        return copy(
            anchor = null,
            found = (found + word.word).toImmutableSet(),
            foundCells = (foundCells + puzzle.runBetween(from, to)).toImmutableSet(),
        )
    }
}
