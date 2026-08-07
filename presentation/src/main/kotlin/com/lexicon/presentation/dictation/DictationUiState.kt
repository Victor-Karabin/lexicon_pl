package com.lexicon.presentation.dictation

import com.lexicon.presentation.common.AnswerState
import com.lexicon.presentation.common.revealedAnswer

sealed interface DictationUiState {
    data object Loading : DictationUiState

    data class Loaded(
        val stepIndex: Int = 0,
        val totalSteps: Int = 0,
        val answerText: String = "",
        val answerState: AnswerState = AnswerState.Unanswered,
        /** Base-language meaning shown once Tip is used, before validation. */
        val tipTranslation: String? = null,
        val tipUsed: Boolean = false,
        val isSessionComplete: Boolean = false,
        /** True while Check/Skip/Next is mid-flight — disables the action row so a rapid double-tap can't fire twice. */
        val isSubmitting: Boolean = false,
    ) : DictationUiState {
        /** Correct (learning-language) answer, shown once the step is validated as Incorrect/Skipped. */
        val revealedAnswer: String? get() = answerState.revealedAnswer
        val isEditable: Boolean get() = answerState is AnswerState.Unanswered
        val canCheck: Boolean get() = isEditable && answerText.isNotBlank() && !isSubmitting
        val canUseTip: Boolean get() = isEditable && !tipUsed && !isSubmitting
        val canSkip: Boolean get() = isEditable && !isSubmitting

        /** Skip auto-advances; only Incorrect requires an explicit Next confirmation. */
        val awaitingNext: Boolean get() = answerState is AnswerState.Incorrect && !isSubmitting
    }
}
