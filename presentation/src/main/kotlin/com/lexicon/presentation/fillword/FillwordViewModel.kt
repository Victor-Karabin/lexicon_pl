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
    /** The first corner of a pair, waiting for the second. */
    val anchor: FillwordCell? = null,
) {
    val total: Int get() = puzzle?.words?.size ?: 0

    val foundCells: Set<FillwordCell>
        get() = puzzle?.words.orEmpty().filter { it.word in found }.flatMap { it.cells }.toSet()

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
     * A word is claimed by tapping its two ends.
     *
     * Tapping rather than dragging: a ten by ten grid on a phone gives cells about
     * thirty dp, and a drag across them is easy to start by accident while scrolling.
     */
    fun onCellTapped(cell: FillwordCell) =
        _uiState.update { state ->
            val puzzle = state.puzzle ?: return@update state
            val anchor = state.anchor
            when {
                anchor == null -> state.copy(anchor = cell)
                anchor == cell -> state.copy(anchor = null)
                else -> {
                    val word = puzzle.wordBetween(anchor, cell)
                    state.copy(
                        anchor = null,
                        found = if (word == null) state.found else (state.found + word.word).toImmutableSet(),
                    )
                }
            }
        }
}
