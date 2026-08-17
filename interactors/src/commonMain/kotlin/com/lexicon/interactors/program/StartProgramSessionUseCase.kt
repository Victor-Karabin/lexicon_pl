package com.lexicon.interactors.program

import com.lexicon.interactors.presets.VocabularyId
import kotlinx.collections.immutable.ImmutableList

interface StartProgramSessionUseCase {
    /** Null when the program has nothing left to offer today. */
    suspend operator fun invoke(id: ProgramId): ProgramSession?
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
