package com.lexicon.interactors.program

import com.lexicon.model.program.ProgramId

interface AdvanceProgramDayUseCase {
    suspend operator fun invoke(id: ProgramId): ProgramDay?
}
