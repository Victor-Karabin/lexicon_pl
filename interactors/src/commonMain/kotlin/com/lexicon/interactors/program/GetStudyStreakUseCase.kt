package com.lexicon.interactors.program

interface GetStudyStreakUseCase {
    suspend operator fun invoke(): Int
}
