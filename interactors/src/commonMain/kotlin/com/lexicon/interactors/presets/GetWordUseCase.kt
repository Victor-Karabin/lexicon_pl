package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word

interface GetWordUseCase {
    suspend operator fun invoke(id: VocabularyId): Word?
}
