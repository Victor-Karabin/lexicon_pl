package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyId

interface GetPinnedImageUseCase {
    suspend operator fun invoke(id: VocabularyId): String?
}
