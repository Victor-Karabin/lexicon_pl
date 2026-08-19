package com.lexicon.interactors.program

import com.lexicon.model.program.ProgramId

interface GetProgramUseCase {
    suspend operator fun invoke(id: ProgramId): Program?
}
