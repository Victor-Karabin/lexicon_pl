package com.lexicon.interactors.presets

interface SetPresetFavouriteUseCase {
    suspend operator fun invoke(
        id: PresetId,
        isFavourite: Boolean,
    )
}
