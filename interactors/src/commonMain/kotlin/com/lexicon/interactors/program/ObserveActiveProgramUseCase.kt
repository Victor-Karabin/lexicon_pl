package com.lexicon.interactors.program

import kotlinx.coroutines.flow.Flow

interface ObserveActiveProgramUseCase {
    operator fun invoke(): Flow<Program?>
}
