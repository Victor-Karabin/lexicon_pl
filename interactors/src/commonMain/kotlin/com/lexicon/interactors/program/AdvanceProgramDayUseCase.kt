package com.lexicon.interactors.program

interface AdvanceProgramDayUseCase {
    suspend operator fun invoke(id: ProgramId): ProgramDay?
}
