package com.lexicon.presentation.common

/** Shared per-step visual state across every training screen. */
enum class AnswerState { UNANSWERED, CORRECT, INCORRECT, SKIPPED }

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
