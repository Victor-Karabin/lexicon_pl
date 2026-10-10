package com.lexicon.data.local

import com.lexicon.model.course.GAP_PATTERN
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonScript
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonScriptsTest {
    private val assets: Map<String, LessonScriptAsset> =
        File("src/androidMain/assets").listFiles { file -> file.name.startsWith("lesson_") }.orEmpty()
            .sortedBy { it.name }
            .associate { it.name to lessonJson.decodeFromString<LessonScriptAsset>(it.readText()) }

    private val scripts: Map<String, LessonScript> = assets.mapValues { (_, asset) -> asset.toModel(emptyMap()) }

    private fun eachScreen(check: (String, LessonScreen) -> Unit) =
        scripts.forEach { (name, script) -> script.screens.forEach { check("$name ${it.id}", it) } }

    @Test
    fun `there is a script for lessons 1 and 2`() {
        assertEquals(listOf("lesson_krok-a1-01.json", "lesson_krok-a1-02.json"), assets.keys.toList())
    }

    @Test
    fun `steps cover every screen once, in order`() {
        scripts.forEach { (name, script) -> assertEquals(name, script.screens.map { it.id }, script.steps.flatMap { it.screenIds }) }
    }

    @Test
    fun `every gap in a dialogue has an answer and every answer a gap`() =
        eachScreen { where, screen ->
            if (screen !is LessonScreen.GapFill) return@eachScreen
            val inText = screen.sections.flatMap { it.lines }.flatMap {
                    line ->
                GAP_PATTERN.findAll(line.text).map { it.groupValues[1] }.toList()
            }
            assertEquals(where, screen.questions.map { it.key }, inText)
        }

    @Test
    fun `every graded item has an answer`() =
        eachScreen { where, screen ->
            screen.questions.forEach { assertTrue("$where ${it.label}", it.answers.isNotEmpty() && it.answers.all(String::isNotBlank)) }
        }

    @Test
    fun `every choice answer is one of its options`() =
        eachScreen { where, screen ->
            if (screen !is LessonScreen.Choice) return@eachScreen
            screen.items.forEach { item -> assertTrue("$where ${item.question.label}", item.question.answers.single() in item.options) }
        }

    @Test
    fun `interchangeable items name real items of their screen`() =
        eachScreen { where, screen ->
            val keys = screen.questions.map { it.key }
            screen.interchangeable.forEach { group -> assertTrue(where, group.size > 1 && group.all { it in keys }) }
        }

    @Test
    fun `the lines to order take every position from 1 once`() =
        eachScreen { where, screen ->
            if (screen !is LessonScreen.Ordering) return@eachScreen
            assertEquals(where, screen.lines.map { it.key }, screen.questions.map { it.key })
            assertEquals(
                where,
                (1..screen.lines.size).map(Int::toString),
                screen.questions.map { it.answers.single() }.sortedBy(String::toInt),
            )
            assertTrue(where, screen.lines.all { it.text.isNotBlank() })
        }

    @Test
    fun `every track a screen plays is declared once and every declared track is played`() =
        assets.forEach { (name, asset) ->
            val ids = asset.tracks.map { it.id }
            assertEquals(name, ids.size, ids.toSet().size)
            val referenced = asset.screens.flatMap { screen ->
                screen.tracks.map { it.id } + screen.groups.mapNotNull { it.track?.id } + screen.sections.mapNotNull { it.track?.id }
            }
            assertEquals(name, emptyList<String>(), referenced.filterNot { it in ids })
            assertEquals(name, emptyList<String>(), ids.filterNot { it in referenced })
        }

    @Test
    fun `every transcript has something to show`() =
        eachScreen { where, screen ->
            val transcript = screen.transcript ?: return@eachScreen
            assertTrue(where, transcript.sections.isNotEmpty())
        }
}
