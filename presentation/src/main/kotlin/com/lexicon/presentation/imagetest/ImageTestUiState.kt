package com.lexicon.presentation.imagetest

import com.lexicon.presentation.common.AnswerState

sealed interface ImageTestUiState {
    data object Loading : ImageTestUiState

    data class Loaded(
        val stepIndex: Int = 0,
        val totalSteps: Int = 0,
        val imageUrl: String? = null,
        val clueText: String = "",
        val options: List<String> = emptyList(),
        val selectedOption: String? = null,
        val correctOption: String? = null,
        val answerState: AnswerState = AnswerState.Unanswered,
        val isSessionComplete: Boolean = false,
    ) : ImageTestUiState {
        val isEditable: Boolean get() = answerState is AnswerState.Unanswered
        val canCheck: Boolean get() = isEditable && selectedOption != null
        val canSkip: Boolean get() = isEditable

        val awaitingNext: Boolean get() = answerState is AnswerState.Incorrect
    }
}
