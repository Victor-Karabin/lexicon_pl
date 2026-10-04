package com.lexicon.data.remote.grammar

import com.lexicon.boundary.GeneratedGrammarBoundary
import com.lexicon.boundary.WordGrammarGenerator

class IosWordGrammarGenerator : WordGrammarGenerator {
    override suspend fun generate(
        text: String,
        translation: String,
    ): GeneratedGrammarBoundary? = null
}
