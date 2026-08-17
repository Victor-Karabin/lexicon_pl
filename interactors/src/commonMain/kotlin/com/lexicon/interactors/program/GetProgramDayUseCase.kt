package com.lexicon.interactors.program

/**
 * Today for a program: the new words, and the queue of trainings.
 *
 * Generated and stored the first time it is asked for on a given day, then read
 * back, so the day does not rearrange itself under the learner.
 */
interface GetProgramDayUseCase {
    suspend operator fun invoke(id: ProgramId): ProgramDay?
}
