package com.lexicon.interactors.program

import com.lexicon.model.program.ProgramId

interface DeleteProgramUseCase {
    suspend operator fun invoke(id: ProgramId)
}
