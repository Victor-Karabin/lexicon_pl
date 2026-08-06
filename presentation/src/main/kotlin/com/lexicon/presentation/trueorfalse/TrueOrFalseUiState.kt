package com.lexicon.presentation.trueorfalse

import com.lexicon.presentation.common.AnswerState

sealed interface TrueOrFalseUiState {
    data object Loading : TrueOrFalseUiState

    data class Loaded(
        val stepIndex: Int = 0,
        val totalSteps: Int = 0,
        val word: String = "",
        val displayedTranslation: String = "",
        val userAnsweredTrue: Boolean? = null,
        val answerState: AnswerState = AnswerState.Unanswered,
        val isSessionComplete: Boolean = false,
    ) : TrueOrFalseUiState {
        val isEditable: Boolean get() = answerState is AnswerState.Unanswered
        val canSkip: Boolean get() = isEditable
        val awaitingNext: Boolean get() = answerState is AnswerState.Incorrect || answerState is AnswerState.Skipped
    }
}
