package com.lexicon.pl.presentation.imagetest

import com.lexicon.pl.presentation.common.AnswerState

data class ImageTestUiState(
    val isLoading: Boolean = true,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val imageUrl: String? = null,
    val clueText: String = "",
    val options: List<String> = emptyList(),
    val selectedOption: String? = null,
    val correctOption: String? = null,
    val answerState: AnswerState = AnswerState.UNANSWERED,
    val isSessionComplete: Boolean = false,
) {
    val isEditable: Boolean get() = answerState == AnswerState.UNANSWERED
    val canCheck: Boolean get() = isEditable && selectedOption != null
    val canSkip: Boolean get() = isEditable
    val awaitingNext: Boolean get() = answerState == AnswerState.INCORRECT || answerState == AnswerState.SKIPPED
}
