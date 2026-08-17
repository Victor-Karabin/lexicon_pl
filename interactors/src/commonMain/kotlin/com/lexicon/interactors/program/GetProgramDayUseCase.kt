package com.lexicon.interactors.program

interface GetProgramDayUseCase {
    suspend operator fun invoke(id: ProgramId): ProgramDay?
}
