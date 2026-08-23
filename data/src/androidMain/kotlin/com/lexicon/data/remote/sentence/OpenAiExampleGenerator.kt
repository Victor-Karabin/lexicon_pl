package com.lexicon.data.remote.sentence

import com.lexicon.boundary.ExampleRequestBoundary
import com.lexicon.boundary.ExampleSentenceGenerator
import com.lexicon.boundary.SentenceResultBoundary

private const val PROMPT = """# Role

You write one short Polish example sentence that shows a learner how a word is used.

## Input

target_word: {{word}}
translation: {{translation}}
language_level: {{level}}

## Behaviour

* Write exactly one natural, contemporary Polish sentence containing the target word.
* Inflect the target word as the grammar requires. Never swap it for a synonym.
* Make the meaning of the target word clear from the sentence itself.
* Keep the vocabulary and grammar at or below the given CEFR level.
* Prefer everyday situations over literary, archaic or regional language.
* Keep it short: eight words or fewer whenever the sentence still sounds natural.

## Output

Return only the sentence, with the target word wrapped in double asterisks in
whatever form it takes.

Example for the target word "kot": Mam czarnego **kota**.

Return no explanation, no translation, no quotation marks and no other Markdown.
"""

class OpenAiExampleGenerator(
    private val api: OpenAiApi,
) : ExampleSentenceGenerator {
    override suspend fun generate(request: ExampleRequestBoundary): SentenceResultBoundary {
        val filled = PROMPT
            .replace("{{word}}", request.word)
            .replace("{{translation}}", request.translation)
            .replace("{{level}}", request.level.ifBlank { "A2" })

        return when (val answer = api.ask(filled)) {
            is OpenAiAnswer.Text ->
                answer.text.trim().takeIf { it.isNotEmpty() }
                    ?.let(SentenceResultBoundary::Generated)
                    ?: SentenceResultBoundary.Refused("empty response")

            OpenAiAnswer.Offline -> SentenceResultBoundary.Offline
            is OpenAiAnswer.Failed -> SentenceResultBoundary.Refused(answer.reason)
        }
    }
}
