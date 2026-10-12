package com.lexicon.application.course

import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonKeysTest {
    private val lessons = File("../data/src/androidMain/assets").listFiles { file -> file.name.startsWith("lesson_") }.orEmpty()
        .sortedBy { it.name }
        .associate { it.name to Json.parseToJsonElement(it.readText()).jsonObject }

    private fun JsonElement.strings(key: String): List<String> =
        when (this) {
            is JsonObject -> entries.flatMap { (name, value) ->
                when {
                    name == key && value is JsonArray -> value.map { it.jsonPrimitive.content }
                    name == key && value is JsonPrimitive && value.isString -> listOf(value.content)
                    else -> value.strings(key)
                }
            }
            is JsonArray -> flatMap { it.strings(key) }
            else -> emptyList()
        }

    @Test
    fun `every lesson is read`() {
        assertEquals((1..7).map { "lesson_krok-a1-0$it.json" }, lessons.keys.toList())
    }

    @Test
    fun `every answer key of every lesson is accepted as written`() =
        lessons.forEach { (name, lesson) ->
            lesson.getValue("screens").jsonArray.forEach { screen ->
                val id = screen.jsonObject.getValue("id").jsonPrimitive.content
                val keys = screen.strings("answer") + screen.strings("answers")
                keys.forEach { key ->
                    assertEquals("$name $id $key", AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(listOf(key), key))
                }
            }
            assertTrue(name, lesson.getValue("screens").jsonArray.isNotEmpty())
        }
}
