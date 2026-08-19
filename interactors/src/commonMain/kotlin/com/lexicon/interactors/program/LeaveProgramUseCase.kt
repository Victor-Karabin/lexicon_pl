package com.lexicon.interactors.program

import com.lexicon.model.program.ProgramId

interface LeaveProgramUseCase {
    suspend operator fun invoke(id: ProgramId)
}
