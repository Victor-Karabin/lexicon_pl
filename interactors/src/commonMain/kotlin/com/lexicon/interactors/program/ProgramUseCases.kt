package com.lexicon.interactors.program

import com.lexicon.interactors.presets.VocabularyId
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

/** The words a program may draw on, in the order its strategy wants them. */
interface ResolveProgramScopeUseCase {
    suspend operator fun invoke(program: Program): ImmutableList<VocabularyId>
}

/**
 * What to do next in a program, ready to hand to a training.
 *
 * Reviews come before new words when any are due: leaving a backlog to grow while
 * meeting new words is how a learner ends up with a hundred words they half-know.
 */
data class ProgramSession(
    val programId: ProgramId,
    val activityId: String,
    val activityType: ActivityType,
    val training: String,
    val wordIds: ImmutableList<VocabularyId>,
)

interface StartProgramSessionUseCase {
    /** Null when the program has nothing left to offer today. */
    suspend operator fun invoke(id: ProgramId): ProgramSession?
}

/** How far through a program the learner is, metric by metric. */
interface GetProgramProgressUseCase {
    suspend operator fun invoke(program: Program): ProgramProgress
}

/** Consecutive days studied, ending today or yesterday. */
interface GetStudyStreakUseCase {
    suspend operator fun invoke(): Int
}

/**
 * Today for a program: the new words, and the queue of trainings.
 *
 * Generated and stored the first time it is asked for on a given day, then read
 * back, so the day does not rearrange itself under the learner.
 */
interface GetProgramDayUseCase {
    suspend operator fun invoke(id: ProgramId): ProgramDay?
}

/** Records that the new-word cards have been through, so they are not shown again. */
interface MarkCardsSeenUseCase {
    suspend operator fun invoke(id: ProgramId)
}

/** The cards for today's new words, with whatever picture each already has. */
interface GetWordCardsUseCase {
    suspend operator fun invoke(ids: List<VocabularyId>): ImmutableList<WordCard>
}
