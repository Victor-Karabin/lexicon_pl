package com.lexicon.interactors.program

/** What is wrong with a draft, in the order the form should complain about it. */
enum class ProgramDraftProblem {
    MISSING_TITLE,

    /** A day with no training in it has nothing to do. */
    NO_TRAININGS,

    /** The study set is what the program teaches, so an empty one teaches nothing. */
    NO_FAVOURITES,
}

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

interface CreateProgramUseCase {
    suspend operator fun invoke(draft: ProgramDraft): Result<Program>
}

/**
 * Rewrites a program the learner already made.
 *
 * The id stays the same, so an enrolment, the days already recorded against it and
 * anything else keyed by it survive the edit — changing your mind about how many
 * words a day is not starting again.
 */
interface UpdateProgramUseCase {
    suspend operator fun invoke(
        id: ProgramId,
        draft: ProgramDraft,
    ): Result<Program>
}

/** Raised by [CreateProgramUseCase] so the form can point at the field at fault. */
class ProgramDraftException(val problem: ProgramDraftProblem) : Exception(problem.name)

/** How many words the study set holds, which is the most a favourites program can teach. */
interface CountFavouritesUseCase {
    suspend operator fun invoke(): Int
}
