package com.lexicon.data.remote.sentence

import com.lexicon.boundary.TranslationDirection
import com.lexicon.boundary.TranslationSuggester

private const val PROMPT = """Give up to {{limit}} natural translations of a word, most common first.

from: {{from}}
to: {{to}}
word: {{word}}

One translation per line. No numbering, no explanation, no punctuation around them."""

class OpenAiTranslationSuggester(
    private val api: OpenAiApi,
) : TranslationSuggester {
    override suspend fun suggest(
        text: String,
        direction: TranslationDirection,
        limit: Int,
    ): List<String> {
        if (limit <= 0) return emptyList()

        val filled = PROMPT
            .replace("{{limit}}", limit.toString())
            .replace("{{from}}", if (direction == TranslationDirection.EN_TO_PL) "English" else "Polish")
            .replace("{{to}}", if (direction == TranslationDirection.EN_TO_PL) "Polish" else "English")
            .replace("{{word}}", text)

        val answer = api.ask(filled) as? OpenAiAnswer.Text ?: return emptyList()
        return answer.text
            .lines()
            .map { it.trim().trim('-', '*', '.', ' ') }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(limit)
    }
}
