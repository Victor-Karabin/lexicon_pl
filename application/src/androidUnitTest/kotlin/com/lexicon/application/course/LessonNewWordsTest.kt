package com.lexicon.application.course

import com.lexicon.interactors.numbers.NumberGender
import com.lexicon.interactors.numbers.NumberKind
import com.lexicon.interactors.numbers.PolishNumbers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private const val SHORTEST_WORD = 3
private const val LARGEST_LESSON_NUMBER = 1000L
private val lookupEndings = listOf("", "?", "!", ".", "…", "…?", "…!")
private val placesOutsideTheCountryList = setOf("Anglia", "USA", "Anglik", "Angielka")

class LessonNewWordsTest {
    private val assets = File("../data/src/androidMain/assets")

    private fun read(name: String): JsonElement = Json.parseToJsonElement(File(assets, name).readText())

    private val vocabulary: Set<String> =
        read("vocabulary_pl.json").jsonArray.map { it.jsonObject.getValue("text").jsonPrimitive.content }.toSet()

    private val numberWords: Set<String> =
        (0..LARGEST_LESSON_NUMBER).flatMap { value ->
            NumberKind.entries.flatMap { kind ->
                (NumberGender.entries + null).mapNotNull { gender -> runCatching { PolishNumbers.words(value, kind, gender) }.getOrNull() }
            }
        }.toSet()

    private val countryWords: Set<String> =
        read("countries.json").jsonArray.flatMap { country ->
            val fields = country.jsonObject
            fields.getValue("polish").jsonArray.map { it.jsonPrimitive.content } +
                listOf("man", "woman", "capital").mapNotNull { key ->
                    fields[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
                }
        }.toSet() + placesOutsideTheCountryList

    private val courseWords = numberWords + countryWords

    private val newWords: List<Pair<String, String>> =
        assets.listFiles { file -> file.name.startsWith("lesson_") }.orEmpty().sortedBy { it.name }.flatMap { file ->
            read(file.name).jsonObject.getValue("screens").jsonArray.flatMap { screen ->
                val fields = screen.jsonObject
                fields["newWords"]?.jsonArray.orEmpty().map { word ->
                    "${file.name} ${fields.getValue("id").jsonPrimitive.content}" to
                        word.jsonObject.getValue("polish").jsonPrimitive.content
                }
            }
        }

    @Test
    fun `every new word is a vocabulary entry, so its row shows level, transcription and status`() {
        val missing = newWords
            .filter { (_, word) -> bare(word).length >= SHORTEST_WORD && word !in courseWords }
            .filter { (_, word) -> spellingsOf(word).none(vocabulary::contains) }
        assertEquals(emptyList<Pair<String, String>>(), missing)
    }

    @Test
    fun `numbers, countries, nationalities and capitals are taught by their courses, not the word list`() {
        assertEquals(emptyList<String>(), courseWords.filter { it in vocabulary })
    }

    @Test
    fun `lessons still list the numbers and countries they introduce`() {
        val listed = newWords.map { it.second }.toSet()
        assertTrue(listed.any { it in numberWords } && listed.any { it in countryWords })
    }

    private fun bare(text: String): String = text.lowercase().trim().trimEnd('?', '!', '.', '…', ' ')

    private fun spellingsOf(text: String): List<String> {
        val base = text.trim().trimEnd('?', '!', '.', '…', ' ')
        return listOf(base, base.replaceFirstChar { it.lowercase() }, base.replaceFirstChar { it.uppercase() })
            .flatMap { spelling -> lookupEndings.map { spelling + it } }
    }
}
