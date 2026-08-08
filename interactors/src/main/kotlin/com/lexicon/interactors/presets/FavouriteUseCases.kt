package com.lexicon.interactors.presets

import kotlinx.coroutines.flow.Flow

/**
 * How much of a preset the user has marked for study.
 *
 * Three states rather than a boolean because a preset can be partly favourited, and a heart
 * that reads "on" for one word out of a thousand would be a lie.
 */
enum class PresetFavouriteState { NONE, SOME, ALL }

/** Marks or unmarks a single word. */
interface ToggleWordFavouriteUseCase {
    suspend operator fun invoke(
        id: VocabularyId,
        isFavourite: Boolean,
    )
}

/**
 * Marks or unmarks every word in a preset at once — the bulk action behind the preset heart.
 */
interface SetPresetFavouriteUseCase {
    suspend operator fun invoke(
        id: PresetId,
        isFavourite: Boolean,
    )
}

/** The favourited ids, emitting on change so every screen showing a heart stays in step. */
interface ObserveFavouriteWordIdsUseCase {
    operator fun invoke(): Flow<Set<VocabularyId>>
}
