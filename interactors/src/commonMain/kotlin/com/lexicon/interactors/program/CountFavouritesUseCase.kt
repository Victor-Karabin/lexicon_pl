package com.lexicon.interactors.program

interface CountFavouritesUseCase {
    suspend operator fun invoke(): Int
}
