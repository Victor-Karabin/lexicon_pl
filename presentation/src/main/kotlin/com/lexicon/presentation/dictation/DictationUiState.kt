package com.lexicon.presentation.dictation

import com.lexicon.presentation.common.AnswerState

data class DictationUiState(
    val isLoading: Boolean = true,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val answerText: String = "",
    val answerState: AnswerState = AnswerState.UNANSWERED,
    /** Shown once Tip is used (pre-validation) or once the step is validated as Incorrect/Skipped. */
    val revealedAnswer: String? = null,
    val tipUsed: Boolean = false,
    val isSessionComplete: Boolean = false,
) {
    val isEditable: Boolean get() = answerState == AnswerState.UNANSWERED
    val canCheck: Boolean get() = isEditable && answerText.isNotBlank()
    val canUseTip: Boolean get() = isEditable && !tipUsed
    val canSkip: Boolean get() = isEditable
    val awaitingNext: Boolean get() = answerState == AnswerState.INCORRECT || answerState == AnswerState.SKIPPED
}
