package com.lexicon.presentation.trueorfalse

import com.lexicon.presentation.common.AnswerState

/** The user answers as many items as they can before this runs out, rather than a fixed step count. */
const val TRUE_OR_FALSE_TIME_LIMIT_SECONDS = 60

sealed interface TrueOrFalseUiState {
    data object Loading : TrueOrFalseUiState

    data class Loaded(
        val stepIndex: Int = 0,
        val timeRemainingSeconds: Int = 0,
        val word: String = "",
        val displayedTranslation: String = "",
        val userAnsweredTrue: Boolean? = null,
        val answerState: AnswerState = AnswerState.Unanswered,
        val isSessionComplete: Boolean = false,
    ) : TrueOrFalseUiState {
        val isEditable: Boolean get() = answerState is AnswerState.Unanswered && !isSessionComplete
    }
}
