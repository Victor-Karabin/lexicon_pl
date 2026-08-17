package com.lexicon.interactors.program

interface UpdateProgramUseCase {
    suspend operator fun invoke(
        id: ProgramId,
        draft: ProgramDraft,
    ): Result<Program>
}
