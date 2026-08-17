package com.lexicon.interactors.program

interface GetProgramUseCase {
    suspend operator fun invoke(id: ProgramId): Program?
}
