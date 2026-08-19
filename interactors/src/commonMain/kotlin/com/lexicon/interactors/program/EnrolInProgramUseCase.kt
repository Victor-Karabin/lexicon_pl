package com.lexicon.interactors.program

import com.lexicon.model.program.ProgramId

interface EnrolInProgramUseCase {
    suspend operator fun invoke(id: ProgramId): ProgramEnrolment
}
