package com.lexicon.pl.presentation.trueorfalse

import com.lexicon.pl.presentation.common.AnswerState

data class TrueOrFalseUiState(
    val isLoading: Boolean = true,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val word: String = "",
    val displayedTranslation: String = "",
    val userAnsweredTrue: Boolean? = null,
    val answerState: AnswerState = AnswerState.UNANSWERED,
    val isSessionComplete: Boolean = false,
) {
    val isEditable: Boolean get() = answerState == AnswerState.UNANSWERED
    val canSkip: Boolean get() = isEditable
    val awaitingNext: Boolean get() = answerState == AnswerState.INCORRECT || answerState == AnswerState.SKIPPED
}
