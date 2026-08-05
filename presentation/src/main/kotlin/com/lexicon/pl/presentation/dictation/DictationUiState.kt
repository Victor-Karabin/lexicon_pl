package com.lexicon.pl.presentation.dictation

enum class DictationAnswerState { UNANSWERED, CORRECT, INCORRECT, SKIPPED }

data class DictationUiState(
    val isLoading: Boolean = true,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val answerText: String = "",
    val answerState: DictationAnswerState = DictationAnswerState.UNANSWERED,
    /** Shown once Tip is used (pre-validation) or once the step is validated as Incorrect/Skipped. */
    val revealedAnswer: String? = null,
    val tipUsed: Boolean = false,
    val isSessionComplete: Boolean = false,
) {
    val isEditable: Boolean get() = answerState == DictationAnswerState.UNANSWERED
    val canCheck: Boolean get() = isEditable && answerText.isNotBlank()
    val canUseTip: Boolean get() = isEditable && !tipUsed
    val canSkip: Boolean get() = isEditable
    val awaitingNext: Boolean get() = answerState == DictationAnswerState.INCORRECT || answerState == DictationAnswerState.SKIPPED
}

sealed interface DictationNavigationEvent {
    data class SessionComplete(val correct: Int, val incorrect: Int, val skipped: Int) : DictationNavigationEvent
}
