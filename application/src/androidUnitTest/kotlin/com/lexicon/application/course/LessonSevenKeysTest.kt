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
import org.junit.Test
import java.io.File

class LessonSevenKeysTest {
    private val screens = Json.parseToJsonElement(File("../data/src/androidMain/assets/lesson_krok-a1-07.json").readText())
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

    @Test
    fun `a coupon is right only with exactly its six numbers, in any order`() {
        val coupon = items("3B").first().list("answers")
        val verdict = { picked: String -> LessonAnswerChecker.verdict(coupon, picked, AnswerMatch.SELECTION) }
        assertEquals(AnswerVerdict.CORRECT, verdict("34 33 20 12 6 5"))
        assertEquals(AnswerVerdict.WRONG, verdict("5 6 12 20 33"))
        assertEquals(AnswerVerdict.WRONG, verdict("5 6 12 20 33 34 35"))
        assertEquals(AnswerVerdict.WRONG, verdict("5 6 12 20 33 35"))
        assertEquals(AnswerVerdict.EMPTY, verdict(""))
    }

    @Test
    fun `a verb takes its book pair or a sensible alternative, never a word that doesn't fit`() {
        val czytać = gap("1C", 1)
        listOf("gazetę", "książkę", "list", "zdanie").forEach {
            assertEquals(it, AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(czytać, it))
        }
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(czytać, "golfa"))
        val gracW = gap("1C", 11)
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gracW, "golfa"))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(gracW, "kawę"))
        val chodzicNa = gap("1C", 9)
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(chodzicNa, "bilet"))
    }

    @Test
    fun `złoty and grosz follow rok, lata, lat`() {
        val keys = items("2D").map {
                item ->
            item.getValue("choices").jsonArray.map { it.jsonObject.getValue("answer").jsonPrimitive.content }
        }
        assertEquals(listOf("złote", "groszy"), keys[0])
        assertEquals(listOf("złotych", "groszy"), keys[1])
        assertEquals(listOf("złotych", "grosze"), keys[4])
        assertEquals(listOf("złotych", "groszy"), keys[5])
        assertEquals(listOf("złote", "groszy"), keys[7])
    }

    @Test
    fun `numbers in words accept spaces between parts, and the workbook dictation also the digits first`() {
        val key = items("3F")[0].list("answers")
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(key, "dwieście  dwanaście"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(key, "212 – dwieście dwanaście"))
        assertEquals(AnswerVerdict.ALMOST, LessonAnswerChecker.verdict(key, "dwiescie dwanascie"))
        val price = items("2G")[5].list("answers")
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(price, "kosztuje dwa złote, pięćdziesiąt groszy"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(price, "kosztuje dwa złote i pięćdziesiąt groszy"))
    }

    @Test
    fun `the washing machine counts in the laundry or in household appliances`() {
        val machine = items("4A")[8].list("answers")
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(machine, "18"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(machine, "19"))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(machine, "17"))
    }

    @Test
    fun `a kiosk request may repeat the Proszę the prompt shows`() {
        val gum = items("2B")[4].list("answers")
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gum, "miętową gumę do żucia"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(gum, "Proszę miętową gumę do żucia."))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(items("2C")[0].list("answers"), "Proszę sok pomarańczowy"))
    }
}
