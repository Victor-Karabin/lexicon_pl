package com.lexicon.interactors.settings

fun interface GetStepCountUseCase {
    suspend operator fun invoke(): Int
}
