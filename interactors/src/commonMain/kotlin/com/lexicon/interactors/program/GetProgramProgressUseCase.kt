package com.lexicon.interactors.program

interface GetProgramProgressUseCase {
    suspend operator fun invoke(program: Program): ProgramProgress
}
