package com.lexicon.interactors.program

import com.lexicon.model.program.ProgramId

fun interface DeleteProgramUseCase {
    suspend operator fun invoke(id: ProgramId)
}
