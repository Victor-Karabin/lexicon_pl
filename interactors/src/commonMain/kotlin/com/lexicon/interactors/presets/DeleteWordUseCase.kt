package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.VocabularyId

fun interface DeleteWordUseCase {
    suspend operator fun invoke(id: VocabularyId)
}
