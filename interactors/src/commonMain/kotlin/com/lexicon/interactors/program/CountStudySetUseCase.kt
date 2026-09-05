package com.lexicon.interactors.program

fun interface CountStudySetUseCase {
    suspend operator fun invoke(): Int
}
