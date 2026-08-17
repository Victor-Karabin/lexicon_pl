package com.lexicon.interactors.program

import kotlinx.coroutines.flow.Flow

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
