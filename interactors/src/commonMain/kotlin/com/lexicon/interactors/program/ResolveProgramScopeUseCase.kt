package com.lexicon.interactors.program

import com.lexicon.interactors.presets.VocabularyId
import kotlinx.collections.immutable.ImmutableList

interface ResolveProgramScopeUseCase {
    suspend operator fun invoke(program: Program): ImmutableList<VocabularyId>
}
