package com.lexicon.interactors.program

import com.lexicon.interactors.presets.VocabularyId
import kotlinx.collections.immutable.ImmutableList

/** The words a program may draw on, in the order its strategy wants them. */
interface ResolveProgramScopeUseCase {
    suspend operator fun invoke(program: Program): ImmutableList<VocabularyId>
}
