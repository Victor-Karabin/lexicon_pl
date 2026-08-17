package com.lexicon.interactors.program

interface CreateProgramUseCase {
    suspend operator fun invoke(draft: ProgramDraft): Result<Program>
}
