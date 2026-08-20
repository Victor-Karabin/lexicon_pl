package com.lexicon.interactors.program

import com.lexicon.model.program.ProgramId

interface GetProgramDayUseCase {
    suspend operator fun invoke(id: ProgramId): ProgramDay?
}
