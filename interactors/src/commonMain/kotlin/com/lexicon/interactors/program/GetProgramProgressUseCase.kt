package com.lexicon.interactors.program

import com.lexicon.model.program.ProgramProgress

interface GetProgramProgressUseCase {
    suspend operator fun invoke(program: Program): ProgramProgress
}
