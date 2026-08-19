package com.lexicon.interactors.program

import com.lexicon.model.program.ProgramId

interface UpdateProgramUseCase {
    suspend operator fun invoke(
        id: ProgramId,
        draft: ProgramDraft,
    ): Result<Program>
}
