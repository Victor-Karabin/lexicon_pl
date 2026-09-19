package com.lexicon.data.remote.sentence

import com.lexicon.boundary.WordLevelGuesser
import com.lexicon.model.vocabulary.CefrLevel

private const val PROMPT = """Reply with the CEFR level a learner of Polish would meet this word at.

word: {{word}}
meaning: {{translation}}

Answer with one of A1, A2, B1, B2, C1, C2 and nothing else."""

class OpenAiWordLevelGuesser(
    private val api: OpenAiApi,
) : WordLevelGuesser {
    override suspend fun guess(
        text: String,
        translation: String,
    ): CefrLevel? {
        val filled = PROMPT
            .replace("{{word}}", text)
            .replace("{{translation}}", translation)

        val answer = api.ask(filled) as? OpenAiAnswer.Text ?: return null
        return CefrLevel.ofName(answer.text.trim().uppercase().take(2))
    }
}
