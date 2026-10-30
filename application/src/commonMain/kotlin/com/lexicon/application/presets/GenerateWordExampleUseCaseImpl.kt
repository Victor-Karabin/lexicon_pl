package com.lexicon.application.presets

import co.touchlab.kermit.Logger
import com.lexicon.boundary.ExampleRequestBoundary
import com.lexicon.boundary.ExampleSentenceGenerator
import com.lexicon.boundary.SentenceResultBoundary
import com.lexicon.common.runSuspendCatching
import com.lexicon.interactors.presets.GenerateWordExampleUseCase
import com.lexicon.model.vocabulary.ExampleSentence

private val log = Logger.withTag("GenerateWordExample")

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
        val result = runSuspendCatching { generator.generate(request) }
            .onFailure { log.w(it) { "An example sentence could not be generated" } }
            .getOrNull()

        return (result as? SentenceResultBoundary.Generated)
            ?.sentence
            ?.takeIf { ExampleSentence.parse(it).text.isNotBlank() }
    }
}
