package com.lexicon.interactors.presets

import kotlinx.coroutines.flow.Flow

interface ObserveFavouriteWordIdsUseCase {
    operator fun invoke(): Flow<Set<VocabularyId>>
}
