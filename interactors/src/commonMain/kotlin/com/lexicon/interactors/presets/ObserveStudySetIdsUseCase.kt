package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.coroutines.flow.Flow

interface ObserveStudySetIdsUseCase {
    operator fun invoke(): Flow<Set<VocabularyId>>
}
