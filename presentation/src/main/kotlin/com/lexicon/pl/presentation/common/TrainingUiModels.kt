package com.lexicon.pl.presentation.common

/** Shared per-step visual state across every training screen. */
enum class AnswerState { UNANSWERED, CORRECT, INCORRECT, SKIPPED }

/** Shared one-time navigation events emitted by training ViewModels. */
sealed interface SessionNavigationEvent {
    data class SessionComplete(val correct: Int, val incorrect: Int, val skipped: Int) : SessionNavigationEvent
}
