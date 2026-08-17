package com.lexicon.interactors.presets

interface ToggleWordFavouriteUseCase {
    suspend operator fun invoke(
        id: VocabularyId,
        isFavourite: Boolean,
    )
}
