package com.lexicon.interactors.program

interface EnrolInProgramUseCase {
    suspend operator fun invoke(id: ProgramId): ProgramEnrolment
}
