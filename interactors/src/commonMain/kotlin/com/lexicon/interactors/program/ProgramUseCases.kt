package com.lexicon.interactors.program

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

interface ObserveProgramsUseCase {
    operator fun invoke(): Flow<ImmutableList<Program>>
}

interface GetProgramUseCase {
    suspend operator fun invoke(id: ProgramId): Program?
}

/**
 * The one program being worked through, if any.
 *
 * One at a time to begin with: a daily plan that had to be reconciled across several
 * programs at once would have to decide whose review budget a shared word came out
 * of, and that is a question worth not asking yet.
 */
interface ObserveActiveEnrolmentUseCase {
    operator fun invoke(): Flow<ProgramEnrolment?>
}

interface EnrolInProgramUseCase {
    suspend operator fun invoke(id: ProgramId): ProgramEnrolment
}

interface LeaveProgramUseCase {
    suspend operator fun invoke(id: ProgramId)
}
