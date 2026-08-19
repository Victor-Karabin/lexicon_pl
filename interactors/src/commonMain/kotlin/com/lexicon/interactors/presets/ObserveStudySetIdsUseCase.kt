package com.lexicon.interactors.presets

import kotlinx.coroutines.flow.Flow

interface ObserveStudySetIdsUseCase {
    operator fun invoke(): Flow<Set<VocabularyId>>
}
