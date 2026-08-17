package com.lexicon.interactors.program

/** Consecutive days studied, ending today or yesterday. */
interface GetStudyStreakUseCase {
    suspend operator fun invoke(): Int
}
