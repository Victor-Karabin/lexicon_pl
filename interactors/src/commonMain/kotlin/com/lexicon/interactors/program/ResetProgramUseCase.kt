package com.lexicon.interactors.program

import com.lexicon.model.program.ProgramId

interface ResetProgramUseCase {
    suspend operator fun invoke(id: ProgramId)
}
