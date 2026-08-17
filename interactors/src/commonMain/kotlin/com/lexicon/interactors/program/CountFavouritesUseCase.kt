package com.lexicon.interactors.program

/** How many words the study set holds, which is the most a favourites program can teach. */
interface CountFavouritesUseCase {
    suspend operator fun invoke(): Int
}
