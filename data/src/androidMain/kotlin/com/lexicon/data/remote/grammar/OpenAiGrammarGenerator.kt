package com.lexicon.data.remote.grammar

import android.util.Log
import com.lexicon.boundary.GeneratedGrammarBoundary
import com.lexicon.boundary.WordGrammarGenerator
import com.lexicon.data.remote.sentence.OpenAiAnswer
import com.lexicon.data.remote.sentence.OpenAiApi
import com.lexicon.data.remote.sentence.ask
import com.lexicon.model.vocabulary.CaseForms
import com.lexicon.model.vocabulary.Gender
import com.lexicon.model.vocabulary.GrammaticalCase
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.WordForms
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val TAG = "GrammarGenerator"

private const val FORM_SEPARATOR = "|"

private val PERSONS = listOf("ja", "ty", "on/ona/ono", "my", "wy", "oni/one")

private const val PROMPT = """# Role

You give a learner the grammar of one Polish word.

## Input

word: {{word}}
translation: {{translation}}

## Behaviour

Decide what kind of word it is, then give the forms that kind needs.

* part_of_speech is exactly one of: n, v, adj, adv, prep, conj, prn, num, part, interj, expr.
* A noun also gets a gender, one of: masculine personal, masculine animate,
  masculine inanimate, feminine, neuter, plural only; and its declension through all
  seven cases, each as the singular and the plural separated by a slash:
  kot -> kot/koty, kota/kotów, kotu/kotom, kota/koty, kotem/kotami, kocie/kotach, kocie/koty.
  A noun with no plural in normal use gets an empty plural in every case; a plural-only
  noun gets an empty singular instead.
* An adjective gets its nominative forms: masculine, feminine, neuter, the masculine
  personal plural and the plural for everything else.
  dobry -> dobry, dobra, dobre, dobrzy, dobre. wysoki -> wysoki, wysoka, wysokie, wysocy, wysokie.
* A verb gets the present tense for all six persons, keeping się where the word has it.
  A perfective verb gets its simple future instead. An impersonal verb such as trzeba
  fills on/ona/ono only.
* Any other kind of word gets the part of speech and nothing else.

## Output

Return only a JSON object, no Markdown and no explanation:

{"part_of_speech": "n", "gender": "masculine animate",
 "declension": {"nominative": "kot/koty", "genitive": "kota/kotów", "dative": "kotu/kotom",
 "accusative": "kota/koty", "instrumental": "kotem/kotami", "locative": "kocie/kotach",
 "vocative": "kocie/koty"},
 "adjective": {"masculine": "", "feminine": "", "neuter": "", "pluralPersonal": "", "pluralOther": ""},
 "conjugation": {"ja": "", "ty": "", "on/ona/ono": "", "my": "", "wy": "", "oni/one": ""}}

Leave out the parts that do not apply to this word.
"""

class OpenAiGrammarGenerator(
    private val api: OpenAiApi,
) : WordGrammarGenerator {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generate(
        text: String,
        translation: String,
    ): GeneratedGrammarBoundary? {
        val filled = PROMPT.replace("{{word}}", text).replace("{{translation}}", translation)

        return when (val answer = api.ask(filled)) {
            is OpenAiAnswer.Text -> answer.text.toGrammar(text)
            OpenAiAnswer.Offline -> null
            is OpenAiAnswer.Failed -> {
                Log.w(TAG, "The grammar of a new word could not be written: ${answer.reason}")
                null
            }
        }
    }

    private fun String.toGrammar(word: String): GeneratedGrammarBoundary? {
        val parsed = runCatching { json.parseToJsonElement(jsonBody()) as? JsonObject }.getOrNull()
        if (parsed == null) {
            Log.w(TAG, "The grammar written for a new word was not JSON")
            return null
        }

        val answered = parsed.string("part_of_speech").trim()
        val partOfSpeech = PartOfSpeech.entries.firstOrNull { it.tag == answered || it.name.equals(answered, ignoreCase = true) }
        if (partOfSpeech == null) {
            Log.w(TAG, "The grammar written for a new word named an unknown part of speech: '$answered'")
            return null
        }
        return GeneratedGrammarBoundary(
            partOfSpeech = partOfSpeech,
            forms = parsed.formsOf(partOfSpeech),
            conjugation = if (word.isBlank()) emptyMap() else parsed.conjugation(),
        )
    }
}

private fun String.jsonBody(): String {
    val start = indexOf('{')
    val end = lastIndexOf('}')
    return if (start in 0..<end) substring(start, end + 1) else this
}

private fun JsonObject.string(name: String): String = (get(name) as? JsonPrimitive)?.content?.takeIf { it != "null" }.orEmpty()

private fun JsonObject.child(name: String): JsonObject? = get(name) as? JsonObject

private fun JsonObject.formsOf(partOfSpeech: PartOfSpeech): WordForms? =
    when (partOfSpeech) {
        PartOfSpeech.NOUN ->
            Gender.ofTag(string("gender"))?.let { gender ->
                WordForms.Noun(gender, child("declension")?.declension().orEmpty())
            }

        PartOfSpeech.ADJECTIVE ->
            child("adjective")
                ?.let { forms ->
                    listOf("masculine", "feminine", "neuter", "pluralPersonal", "pluralOther").map(forms::string)
                }?.takeIf { forms -> forms.all { it.isNotBlank() } }
                ?.let { WordForms.Adjective(it[0], it[1], it[2], it[3], it[4]) }

        else -> null
    }

private fun JsonObject.declension(): Map<GrammaticalCase, CaseForms> =
    GrammaticalCase.entries.mapNotNull { case ->
        val parts = string(case.tag).split("/", FORM_SEPARATOR)
        CaseForms(parts.firstOrNull()?.trim()?.ifBlank { null }, parts.getOrNull(1)?.trim()?.ifBlank { null })
            .takeUnless { it.isEmpty }
            ?.let { case to it }
    }.toMap()

private fun JsonObject.conjugation(): Map<String, List<String>> =
    child("conjugation")
        ?.let { forms -> PERSONS.associateWith { person -> forms.string(person).trim() } }
        ?.filterValues { it.isNotEmpty() }
        ?.mapValues { (_, form) -> listOf(form) }
        .orEmpty()
