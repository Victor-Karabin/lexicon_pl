package com.lexicon.boundary

import com.lexicon.model.vocabulary.Aspect
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.WordForms

data class GeneratedGrammarBoundary(
    val partOfSpeech: PartOfSpeech,
    val forms: WordForms? = null,
    val conjugation: Map<String, List<String>> = emptyMap(),
    val aspect: Aspect? = null,
)

interface WordGrammarGenerator {
    suspend fun generate(
        text: String,
        translation: String,
    ): GeneratedGrammarBoundary?
}
