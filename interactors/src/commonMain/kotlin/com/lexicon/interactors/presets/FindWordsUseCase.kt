package com.lexicon.interactors.presets

import com.lexicon.model.vocabulary.Word

fun interface FindWordsUseCase {
    suspend operator fun invoke(texts: List<String>): Map<String, Word>
}
