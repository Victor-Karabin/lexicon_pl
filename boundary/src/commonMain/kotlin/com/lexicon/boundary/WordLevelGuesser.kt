package com.lexicon.boundary

import com.lexicon.model.vocabulary.CefrLevel

interface WordLevelGuesser {
    suspend fun guess(
        text: String,
        translation: String,
    ): CefrLevel?
}
