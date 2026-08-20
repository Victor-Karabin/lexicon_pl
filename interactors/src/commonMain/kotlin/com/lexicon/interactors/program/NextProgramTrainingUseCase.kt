package com.lexicon.interactors.program

import com.lexicon.model.program.ProgramId
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList

data class ProgramLaunch(
    val training: TrainingType,
    val wordIds: ImmutableList<VocabularyId>,
)

interface NextProgramTrainingUseCase {
    suspend fun next(id: ProgramId): ProgramLaunch?

    suspend fun advance(id: ProgramId): ProgramLaunch?
}
