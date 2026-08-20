package com.lexicon.presentation.common

sealed interface AnswerState {
    data object Unanswered : AnswerState

    data object Correct : AnswerState

    data class Incorrect(val revealedAnswer: String? = null) : AnswerState

    data class Skipped(val revealedAnswer: String? = null) : AnswerState
}

val AnswerState.revealedAnswer: String?
    get() = when (this) {
        is AnswerState.Incorrect -> revealedAnswer
        is AnswerState.Skipped -> revealedAnswer
        AnswerState.Correct, AnswerState.Unanswered -> null
    }

sealed interface SessionNavigationEvent {
    data class SessionComplete(
        val correct: Int,
        val incorrect: Int,
        val skipped: Int,
        val tipsUsed: Int = 0,
    ) : SessionNavigationEvent
}

data class WordResultEntry(
    val word: String,
    val translation: String,
    val outcome: AnswerState,
    val tipUsed: Boolean = false,
)
