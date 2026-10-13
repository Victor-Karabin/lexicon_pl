package com.lexicon.application.course

import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.model.course.AnswerMatch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonSixKeysTest {
    private val lesson = LessonAsset(6)

    private fun JsonObject.keys(): List<String> =
        listOf(getValue("answer").jsonPrimitive.content) + (get("answers")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList())

    @Test
    fun `a word that fits two frames is right in either`() {
        val film = lesson.items("4E").first { it.getValue("prompt").jsonPrimitive.content == "film" }.keys()
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(film, "oglądam"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(film, "chodzę na"))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(film, "gram w"))
    }

    @Test
    fun `the activities of a person count in any order, joined by commas or i`() {
        val jonatan = lesson.gap("4D", 4)
        listOf(
            "biega, pływa i jeździ na rowerze",
            "jeździ na rowerze, biega i pływa",
            "pływa i biega i jeździ na rowerze",
            "biega, jeździ na rowerze, pływa",
        ).forEach { assertEquals(it, AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(jonatan, it)) }
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(jonatan, "biega i pływa"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(lesson.gap("4D", 1), "czyta i słucha muzyki"))
    }

    @Test
    fun `a text message rewritten without one Polish letter is wrong, not almost`() {
        listOf("5D", "5F").forEach { id ->
            lesson.items(id).forEach { item ->
                val key = item.list("answers")
                val unaccented = item.getValue("prompt").jsonPrimitive.content
                assertEquals(id, AnswerVerdict.WRONG, LessonAnswerChecker.verdict(key, unaccented, AnswerMatch.EXACT_LETTERS))
                assertEquals(id, AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(key, key.single(), AnswerMatch.EXACT_LETTERS))
            }
        }
        val reply = lesson.items("5D").first { it.getValue("label").jsonPrimitive.content == "a" }.list("answers")
        val withoutSmiley = "ja nie mogę uczę się hiszpańskiego przepraszam ale javier mówi że on ma czas na kawę"
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(reply, withoutSmiley, AnswerMatch.EXACT_LETTERS))
        val oneMissing = "Ja nie mogę, uczę się hiszpańskiego. Przepraszam! Ale Javier mowi, że on ma czas na kawę :-)"
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(reply, oneMissing, AnswerMatch.EXACT_LETTERS))
    }

    @Test
    fun `the odd form is one of the three printed forms and its feedback names its person`() {
        listOf("3D", "3E").forEach { id ->
            lesson.items(id).forEach { item ->
                val options = item.list("options")
                val odd = item.keys().single()
                assertEquals(id, 3, options.size)
                assertTrue(id, odd in options)
                assertEquals(id, AnswerVerdict.WRONG, LessonAnswerChecker.verdict(listOf(odd), options.first { it != odd }))
            }
        }
    }
}
