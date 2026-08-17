package com.lexicon.interactors.program

import kotlinx.coroutines.flow.Flow

interface ObserveActiveEnrolmentUseCase {
    operator fun invoke(): Flow<ProgramEnrolment?>
}
