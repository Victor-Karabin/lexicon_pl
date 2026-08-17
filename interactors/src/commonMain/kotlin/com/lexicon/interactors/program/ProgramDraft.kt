package com.lexicon.interactors.program

/**
 * A program the learner wrote, over the words they have starred.
 *
 * Only the parts worth choosing are here. Everything else a program needs — the
 * scope, the goal, the review schedule, how progress is weighed — follows from the
 * study set and is filled in when the draft is turned into a program, because a form
 * asking for a retention weight is a form nobody finishes.
 */
data class ProgramDraft(
    val title: String,
    val description: String = "",
    val newWordsPerDay: Int,
    val reviewWordsPerDay: Int,
    /** The trainings the day works through, in the order they come. */
    val trainings: List<String> = emptyList(),
)
