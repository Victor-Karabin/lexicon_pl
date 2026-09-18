package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.WordStatus

interface SetWordStatusUseCase {
    suspend operator fun invoke(
        id: VocabularyId,
        status: WordStatus,
    )
}
