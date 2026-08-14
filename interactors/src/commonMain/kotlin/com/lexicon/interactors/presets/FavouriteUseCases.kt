package com.lexicon.interactors.presets

import kotlinx.coroutines.flow.Flow

enum class PresetFavouriteState { NONE, SOME, ALL }

interface ToggleWordFavouriteUseCase {
    suspend operator fun invoke(
        id: VocabularyId,
        isFavourite: Boolean,
    )
}

interface SetPresetFavouriteUseCase {
    suspend operator fun invoke(
        id: PresetId,
        isFavourite: Boolean,
    )
}

interface ObserveFavouriteWordIdsUseCase {
    operator fun invoke(): Flow<Set<VocabularyId>>
}
