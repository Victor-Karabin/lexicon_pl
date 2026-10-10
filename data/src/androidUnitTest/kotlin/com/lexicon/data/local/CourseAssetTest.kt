package com.lexicon.data.local

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CourseAssetTest {
    private fun asset(name: String) = Json.parseToJsonElement(File("src/androidMain/assets/$name").readText())

    private val vocabulary: Map<Long, String> =
        asset("vocabulary_pl.json").jsonArray.associate { word ->
            word.jsonObject.getValue("id").jsonPrimitive.long to word.jsonObject.getValue("text").jsonPrimitive.content
        }

    private val lessons: List<JsonObject> =
        asset("course_krok.json").jsonObject.getValue("courses").jsonArray.flatMap { course ->
            course.jsonObject.getValue("lessons").jsonArray.map { it.jsonObject }
        }

    private fun JsonObject.strings(key: String) = getValue(key).jsonArray.map { it.jsonPrimitive.content }

    @Test
    fun `every lesson word link still points at the word the course was built for`() {
        val drifted =
            lessons.flatMap { lesson ->
                val ids = lesson.getValue("vocabularyIds").jsonArray.map { it.jsonPrimitive.long }
                val words = lesson.strings("vocabularyWords")
                assertEquals("${lesson["id"]}: one recorded word per id", ids.size, words.size)
                ids.zip(words).filter { (id, word) -> vocabulary[id] != word }.map { (id, word) ->
                    "${lesson["id"]}: id $id is now '${vocabulary[id]}', built as '$word'"
                }
            }
        assertTrue("Rerun tools/course/build_course.py after changing the corpus:\n" + drifted.joinToString("\n"), drifted.isEmpty())
    }

    @Test
    fun `every recording a lesson script plays from the books can be downloaded`() {
        val remoteIds =
            lessons.associate { lesson ->
                lesson.getValue("id").jsonPrimitive.content to
                    lesson.getValue("audio").jsonArray.associate {
                        it.jsonObject.getValue("file").jsonPrimitive.content to it.jsonObject["remoteId"]?.jsonPrimitive?.contentOrNull
                    }
            }
        val unreachable =
            File("src/androidMain/assets").listFiles { file -> file.name.startsWith("lesson_") }.orEmpty().flatMap { script ->
                val lessonId = script.name.removePrefix("lesson_").removeSuffix(".json")
                asset(script.name).jsonObject.getValue("tracks").jsonArray
                    .map { it.jsonObject.getValue("file").jsonPrimitive.content }
                    .filter { it.startsWith("a1_workbook_") }
                    .filter { remoteIds[lessonId]?.get(it) == null }
                    .map { "$lessonId: $it" }
            }
        assertTrue(
            "Rerun tools/course/fetch_drive_manifest.py and build_course.py:\n" + unreachable.joinToString("\n"),
            unreachable.isEmpty(),
        )
        assertEquals(emptyList<String>(), remoteIds.values.flatMap { tracks -> tracks.filterValues { it == null }.keys })
    }

    @Test
    fun `lesson 1 trains the phrases its script teaches`() {
        val words = lessons.first { it["id"]?.jsonPrimitive?.content == "krok-a1-01" }.strings("vocabularyWords")
        val taught =
            listOf(
                "dzień dobry", "cześć", "nazywam się", "mam na imię", "bardzo mi miło", "miło mi", "mnie również", "a ty?",
                "jak się pan nazywa?", "jak się pani nazywa?", "jak masz na imię?", "nie rozumiem", "proszę powtórzyć",
                "przepraszam", "dziękuję", "szkoła", "nauczycielka", "zero", "pięć", "dziesięć",
            )
        assertEquals(emptyList<String>(), taught.filterNot { it in words })
    }
}
