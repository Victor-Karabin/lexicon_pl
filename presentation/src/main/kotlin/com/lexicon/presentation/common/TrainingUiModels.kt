package com.lexicon.presentation.common

/** Shared per-step visual state across every training screen. */
sealed interface AnswerState {
    data object Unanswered : AnswerState

    data object Correct : AnswerState

    /** [revealedAnswer] is null for trainings that don't reveal a correct-answer string (e.g. True or False). */
    data class Incorrect(val revealedAnswer: String? = null) : AnswerState

    data class Skipped(val revealedAnswer: String? = null) : AnswerState
}

/** The correct-answer text revealed once a step is validated as Incorrect/Skipped, if this training reveals one. */
val AnswerState.revealedAnswer: String?
    get() = when (this) {
        is AnswerState.Incorrect -> revealedAnswer
        is AnswerState.Skipped -> revealedAnswer
        AnswerState.Correct, AnswerState.Unanswered -> null
    }

/** Shared one-time navigation events emitted by training ViewModels. */
sealed interface SessionNavigationEvent {
    data class SessionComplete(
        val correct: Int,
        val incorrect: Int,
        val skipped: Int,
        val tipsUsed: Int = 0,
    ) : SessionNavigationEvent
}

/** One vocabulary item's outcome for the Results screen's word-level breakdown. Outcome is always terminal (never UNANSWERED). */
data class WordResultEntry(
    val word: String,
    val translation: String,
    val outcome: AnswerState,
    val tipUsed: Boolean = false,
)
