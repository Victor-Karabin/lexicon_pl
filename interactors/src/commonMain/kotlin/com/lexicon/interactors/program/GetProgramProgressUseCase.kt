package com.lexicon.interactors.program

/** How far through a program the learner is, metric by metric. */
interface GetProgramProgressUseCase {
    suspend operator fun invoke(program: Program): ProgramProgress
}
