package com.lexicon.application.course

import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.model.course.AnswerMatch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.File

class LessonNineKeysTest {
    private val screens = Json.parseToJsonElement(File("../data/src/androidMain/assets/lesson_krok-a1-09.json").readText())
        .jsonObject.getValue("screens").jsonArray.map { it.jsonObject }

    private fun screen(id: String): JsonObject = screens.first { it.getValue("id").jsonPrimitive.content == id }

    private fun JsonObject.list(key: String): List<String> = getValue(key).jsonArray.map { it.jsonPrimitive.content }

    private fun items(id: String) = screen(id).getValue("items").jsonArray.map { it.jsonObject }

    private fun gap(
        id: String,
        number: Int,
    ): List<String> =
        screen(id).getValue("gaps").jsonArray.map { it.jsonObject }
            .first { it.getValue("number").jsonPrimitive.content == number.toString() }.list("answers")

    private fun written(
        id: String,
        index: Int,
        typed: String,
        match: AnswerMatch = AnswerMatch.WHOLE,
    ) = LessonAnswerChecker.verdict(items(id)[index].list("answers"), typed, match)

    @Test
    fun `a salad is right with its ingredients in any order, with or without a repeated z`() {
        assertEquals(AnswerVerdict.CORRECT, written("1E", 0, "marchewka z selerem, z jabłkiem i z cytryną", AnswerMatch.ANY_ORDER))
        assertEquals(AnswerVerdict.CORRECT, written("1E", 0, "marchewka z cytryną, jabłkiem i selerem", AnswerMatch.ANY_ORDER))
        assertEquals(AnswerVerdict.CORRECT, written("1E", 3, "Sałata z ogórkiem, z rzodkiewką i ze śmietaną", AnswerMatch.ANY_ORDER))
        assertNotEquals(AnswerVerdict.CORRECT, written("1E", 0, "marchewka z selerem i z jabłkiem", AnswerMatch.ANY_ORDER))
        assertNotEquals(AnswerVerdict.CORRECT, written("1E", 0, "marchewka z seler, z jabłko i z cytryna", AnswerMatch.ANY_ORDER))
    }

    @Test
    fun `plural forms follow the ending rules and their traps`() {
        assertEquals(AnswerVerdict.CORRECT, written("3B", 3, "ziemniaki"))
        assertNotEquals(AnswerVerdict.CORRECT, written("3B", 3, "ziemniaky"))
        assertEquals(AnswerVerdict.CORRECT, written("3B", 11, "cebule"))
        assertEquals(AnswerVerdict.CORRECT, written("3B", 12, "jajka"))
        assertEquals(AnswerVerdict.CORRECT, written("3B", 19, "żółte papryki"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gap("5D", 5), "małe cebule"))
        assertNotEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gap("5D", 5), "małe cebuly"))
    }

    @Test
    fun `two takes dwie with feminine nouns`() {
        assertEquals(AnswerVerdict.CORRECT, written("3D", 9, "wody mineralne"))
        assertEquals(AnswerVerdict.CORRECT, written("3D", 9, "dwie wody mineralne"))
        assertNotEquals(AnswerVerdict.CORRECT, written("3D", 9, "dwa wody mineralne"))
        assertEquals(AnswerVerdict.CORRECT, written("3D", 0, "dwa jasne chleby"))
        assertNotEquals(AnswerVerdict.CORRECT, written("3D", 0, "dwie jasne chleby"))
        assertEquals(AnswerVerdict.CORRECT, written("3E", 4, "2 duże kapusty"))
    }

    @Test
    fun `the reason is right with or without bo to jest`() {
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gap("5C", 1), "mięso"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gap("5C", 1), "bo to jest mięso"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gap("5C", 4), "owoce"))
        assertNotEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gap("5C", 3), "mięso"))
    }

    @Test
    fun `fruit names take the animate accusative`() {
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gap("5D", 14), "ananasa"))
        assertNotEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gap("5D", 14), "ananas"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gap("5D", 15), "małego arbuza"))
    }

    @Test
    fun `sentences may swap the compared or listed things`() {
        assertEquals(AnswerVerdict.CORRECT, written("5E", 2, "Wolę jagody niż winogrona."))
        assertEquals(AnswerVerdict.CORRECT, written("5E", 4, "czy mogę prosić łyżeczkę i kubek"))
    }
}
