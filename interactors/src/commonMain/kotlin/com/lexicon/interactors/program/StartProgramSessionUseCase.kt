package com.lexicon.interactors.program

import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList

interface StartProgramSessionUseCase {
    suspend operator fun invoke(id: ProgramId): ProgramSession?
}

data class ProgramSession(
    val programId: ProgramId,
    val activityId: String,
    val activityType: ActivityType,
    val training: String,
    val wordIds: ImmutableList<VocabularyId>,
)
