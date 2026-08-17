package com.lexicon.interactors.program

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
