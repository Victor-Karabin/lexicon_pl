package com.lexicon.presentation.dictation

import com.lexicon.presentation.common.AnswerState

data class DictationUiState(
    val isLoading: Boolean = true,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val answerText: String = "",
    val answerState: AnswerState = AnswerState.UNANSWERED,
    /** Base-language meaning shown once Tip is used, before validation. */
    val tipTranslation: String? = null,
    /** Correct (learning-language) answer, shown once the step is validated as Incorrect/Skipped. */
    val revealedAnswer: String? = null,
    val tipUsed: Boolean = false,
    val isSessionComplete: Boolean = false,
    /** True while Check/Skip/Next is mid-flight — disables the action row so a rapid double-tap can't fire twice. */
    val isSubmitting: Boolean = false,
) {
    val isEditable: Boolean get() = answerState == AnswerState.UNANSWERED
    val canCheck: Boolean get() = isEditable && answerText.isNotBlank() && !isSubmitting
    val canUseTip: Boolean get() = isEditable && !tipUsed && !isSubmitting
    val canSkip: Boolean get() = isEditable && !isSubmitting

    /** Skip auto-advances; only Incorrect requires an explicit Next confirmation. */
    val awaitingNext: Boolean get() = answerState == AnswerState.INCORRECT && !isSubmitting
}
