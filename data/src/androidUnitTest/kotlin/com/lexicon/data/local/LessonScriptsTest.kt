package com.lexicon.data.local

import com.lexicon.model.course.GAP_PATTERN
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonScript
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private const val STEM = 4
private const val SHORTEST_WORD = 3
private val afterSubmit = setOf("newWords", "model", "checklist")

class LessonScriptsTest {
    private val assets: Map<String, LessonScriptAsset> =
        File("src/androidMain/assets").listFiles { file -> file.name.startsWith("lesson_") }.orEmpty()
            .sortedBy { it.name }
            .associate { it.name to lessonJson.decodeFromString<LessonScriptAsset>(it.readText()) }

    private val scripts: Map<String, LessonScript> = assets.mapValues { (_, asset) -> asset.toModel(emptyMap()) }

    private fun eachScreen(check: (String, LessonScreen) -> Unit) =
        scripts.forEach { (name, script) -> script.screens.forEach { check("$name ${it.id}", it) } }

    @Test
    fun `there is a script for lessons 1 to 3`() {
        assertEquals(listOf("lesson_krok-a1-01.json", "lesson_krok-a1-02.json", "lesson_krok-a1-03.json"), assets.keys.toList())
    }

    @Test
    fun `every lesson opens with its ungraded grammar screen in a step of its own`() =
        scripts.forEach { (name, script) ->
            val grammar = script.steps.first()
            assertTrue(name, grammar.isGrammar && grammar.screenIds == listOf("0"))
            assertTrue(name, script.steps.drop(1).none { it.isGrammar })
            assertTrue(name, script.screens.first() is LessonScreen.Grammar)
            assertTrue(name, script.rules.isNotEmpty() && script.rules.map { it.number } == (1..script.rules.size).toList())
            assertTrue(name, script.rules.all { it.title.isNotBlank() && (it.tables.isNotEmpty() || it.notes.isNotEmpty()) })
        }

    @Test
    fun `rule links point at rules and screens that exist`() =
        scripts.forEach { (name, script) ->
            val ids = script.screens.map { it.id }
            script.ruleLinks.forEach { (screenId, numbers) ->
                assertTrue("$name $screenId", screenId in ids && screenId != "0")
                assertEquals("$name $screenId", numbers.size, script.rulesOf(screenId).size)
            }
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
    fun `no choice screen is answered diagonally, item N by option N`() =
        eachScreen { where, screen ->
            if (screen !is LessonScreen.Choice || screen.items.size < 3) return@eachScreen
            val shared = screen.items.all { it.options == screen.items.first().options }
            if (!shared) return@eachScreen
            assertTrue(
                where,
                screen.items.withIndex().any {
                        (index, item) ->
                    item.question.answers.single() != item.options.getOrNull(index)
                },
            )
        }

    @Test
    fun `new words belong to screens of the lesson and are never empty`() =
        scripts.forEach { (name, script) ->
            val ids = script.screens.map { it.id }
            script.newWords.forEach { (id, newWords) ->
                assertTrue("$name $id", id in ids && newWords.words.isNotEmpty())
                assertTrue("$name $id", newWords.words.all { it.polish.isNotBlank() && it.english.isNotBlank() })
            }
        }

    @Test
    fun `every typed gap stands inside its sentence, never after a prompt with an ellipsis`() =
        eachScreen { where, screen ->
            if (screen !is LessonScreen.Write && screen !is LessonScreen.Form) return@eachScreen
            screen.questions.forEach { assertTrue("$where ${it.label}", it.prompt?.contains('…') != true) }
        }

    @Test
    fun `every inline choice line has one option group per marker and each answer among its options`() =
        eachScreen { where, screen ->
            if (screen !is LessonScreen.InlineChoice) return@eachScreen
            screen.lines.forEach { line ->
                assertEquals("$where ${line.label}", line.groups.size, GAP_PATTERN.findAll(line.text).count())
                line.groups.forEach { assertTrue("$where ${line.label}", it.question.answers.single() in it.options) }
            }
        }

    @Test
    fun `new words appear in the text their screen shows before the answers`() =
        assets.forEach { (name, asset) ->
            asset.screens.filter { it.newWords.isNotEmpty() }.forEach { screen ->
                val shown = visibleText(lessonJson.encodeToJsonElement(ScreenAsset.serializer(), screen)).lowercase()
                screen.newWords.forEach { word ->
                    val words = word.polish.lowercase().split(
                        ' ',
                        '/',
                        ',',
                        '·',
                    ).map { it.trim('.', '?', '!', '…') }.filter { it.isNotEmpty() }
                    val stems = words.filter { it.length >= SHORTEST_WORD }.ifEmpty { words }
                    assertTrue("$name ${screen.id} ${word.polish}", stems.any { shown.contains(it.take(STEM)) })
                }
            }
        }

    private fun visibleText(element: JsonElement): String =
        when (element) {
            is JsonObject -> element.filterKeys { it !in afterSubmit }.values.joinToString(" ") { visibleText(it) }
            is JsonArray -> element.joinToString(" ") { visibleText(it) }
            is JsonPrimitive -> if (element.isString) element.content else ""
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
    fun `free writing asks for several lines only where its model answer spans lines`() {
        val multiline = scripts.flatMap { (name, script) ->
            script.screens.filterIsInstance<LessonScreen.FreeWriting>().flatMap { screen ->
                screen.fields.indices.filter(screen::isMultiline).map { "$name ${screen.id} $it" }
            }
        }
        assertEquals(listOf("lesson_krok-a1-01.json 2D 0", "lesson_krok-a1-01.json 2D 1"), multiline)
    }

    @Test
    fun `every transcript has something to show`() =
        eachScreen { where, screen ->
            val transcript = screen.transcript ?: return@eachScreen
            assertTrue(where, transcript.sections.isNotEmpty())
        }
}
