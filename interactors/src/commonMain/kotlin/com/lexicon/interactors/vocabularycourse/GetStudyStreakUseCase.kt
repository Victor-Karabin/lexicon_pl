package com.lexicon.interactors.vocabularycourse

interface GetStudyStreakUseCase {
    suspend operator fun invoke(): Int
}
