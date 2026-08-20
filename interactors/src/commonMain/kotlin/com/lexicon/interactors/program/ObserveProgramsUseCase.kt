package com.lexicon.interactors.program

import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

interface ObserveProgramsUseCase {
    operator fun invoke(): Flow<ImmutableList<Program>>
}
