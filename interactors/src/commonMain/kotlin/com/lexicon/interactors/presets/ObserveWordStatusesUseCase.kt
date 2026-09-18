package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.WordStatus
import kotlinx.coroutines.flow.Flow

interface ObserveWordStatusesUseCase {
    operator fun invoke(): Flow<Map<VocabularyId, WordStatus>>
}
