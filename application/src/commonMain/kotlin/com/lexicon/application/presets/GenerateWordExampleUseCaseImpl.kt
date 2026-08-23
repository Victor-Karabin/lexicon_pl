package com.lexicon.application.presets

import com.lexicon.boundary.ExampleRequestBoundary
import com.lexicon.boundary.ExampleSentenceGenerator
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.interactors.presets.GenerateWordExampleUseCase
import com.lexicon.model.vocabulary.ExampleSentence

class GenerateWordExampleUseCaseImpl(
    private val generator: ExampleSentenceGenerator,
) : GenerateWordExampleUseCase {
    override suspend fun invoke(
        text: String,
        translation: String,
        level: String,
    ): String? {
        val word = text.trim()
        if (word.isEmpty()) return null

        val request = ExampleRequestBoundary(word = word, translation = translation.trim(), level = level)
        val result = runCatching { generator.generate(request) }.getOrNull()

        return (result as? SentenceResultBoundary.Generated)
            ?.sentence
            ?.takeIf { ExampleSentence.parse(it).text.isNotBlank() }
    }
}
