package com.lexicon.interactors.program

interface LeaveProgramUseCase {
    suspend operator fun invoke(id: ProgramId)
}
