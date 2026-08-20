package com.lexicon.interactors.program

interface CountStudySetUseCase {
    suspend operator fun invoke(): Int
}
