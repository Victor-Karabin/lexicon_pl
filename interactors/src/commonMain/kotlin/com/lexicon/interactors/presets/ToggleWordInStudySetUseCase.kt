package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyId

interface ToggleWordInStudySetUseCase {
    suspend operator fun invoke(
        id: VocabularyId,
        isInStudySet: Boolean,
    )
}
