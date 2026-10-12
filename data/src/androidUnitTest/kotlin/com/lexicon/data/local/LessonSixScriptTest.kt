package com.lexicon.data.local

import com.lexicon.model.course.AnswerMatch
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.TranscriptUnlock
import com.lexicon.model.course.allTracks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonSixScriptTest {
    private val asset = lessonJson.decodeFromString<LessonScriptAsset>(File("src/androidMain/assets/lesson_krok-a1-06.json").readText())
    private val script = asset.toModel(asset.tracks.associate { it.file to "remote-${it.id}" })

    private fun screen(id: String) = script.screens.first { it.id == id }

    @Test
    fun `lesson 6 has 38 screens, the grammar screen and six steps, the last one a review`() {
        assertEquals(38, script.screens.size)
        assertEquals(listOf("G", "1", "2", "3", "4", "5", "6"), script.steps.map { it.id })
        assertEquals(listOf("6A", "6B"), script.steps.single { it.isReview }.screenIds)
        assertEquals(5, script.rules.size)
        assertEquals(31, script.ruleLinks.size)
        assertTrue(script.screens.flatMap { it.allTracks() }.all { it.remoteId != null })
    }

    @Test
    fun `graded screens hold the number of items the plan asks for`() {
        val counts = script.screens.filter { it.isGraded }.associate { it.id to it.questions.size }
        val expected = mapOf(
            "1A" to 5, "1B" to 5, "1C" to 5, "1D" to 5, "1E" to 5,
            "2B" to 22, "2C" to 15,
            "3B" to 23, "3C" to 36, "3D" to 9, "3E" to 9, "3F" to 9, "3G" to 10, "3H" to 10, "3I" to 5, "3J" to 10,
            "4A" to 9, "4C" to 19, "4D" to 5, "4E" to 17, "4F" to 4, "4G" to 5, "4H" to 5, "4I" to 6, "4J" to 4,
            "5A" to 4, "5B" to 7, "5C" to 5, "5D" to 8, "5E" to 4, "5F" to 6,
            "6A" to 10, "6B" to 8,
        )
        assertEquals(expected, counts)
    }

    @Test
    fun `each dialogue screen asks for its picture with the same four options, then fills four gaps`() {
        val pictures = listOf("1A", "1B", "1C", "1D").map { id ->
            val screen = screen(id) as LessonScreen.GapFill
            val choice = screen.choices.single()
            assertEquals(listOf("📷 photo album", "📖 book", "🥟 dough", "💻 laptop"), choice.options)
            assertEquals(choice.question, screen.questions.first())
            assertEquals(4, screen.questions.size - 1)
            assertEquals(TranscriptUnlock.AfterCheck, screen.transcript?.unlock)
            assertTrue(!screen.hint.isNullOrBlank())
            choice.question.answers.single()
        }
        assertEquals(listOf("🥟 dough", "📖 book", "💻 laptop", "📷 photo album"), pictures)
    }

    @Test
    fun `every conjugation form is typed, none is given`() {
        val tables = (screen("3C") as LessonScreen.Conjugation).tables
        assertEquals(listOf("mieć", "rozumieć", "robić", "tańczyć", "pisać", "gotować"), tables.map { it.verb })
        assertTrue(tables.all { table -> table.rows.size == 6 && table.rows.all { it.answers.isNotEmpty() } })
        assertEquals(listOf("rozumieją"), tables[1].rows[5].answers)
    }

    @Test
    fun `odd one out shows three printed forms per item and names the odd form's person`() {
        listOf("3D", "3E").forEach { id ->
            val screen = screen(id) as LessonScreen.Choice
            assertTrue(id, !screen.shuffle)
            screen.items.forEach { item ->
                assertEquals(id, 3, item.options.size)
                val odd = item.question.answers.single()
                assertTrue(id, odd in item.options)
                assertTrue(id, item.question.feedback.orEmpty().startsWith("*$odd* ="))
            }
        }
    }

    @Test
    fun `4E sorts every word into the seven frames, and film, mecz and spektakl fit two`() {
        val screen = screen("4E") as LessonScreen.Choice
        val twoFrames = screen.items.filter { it.question.answers.size == 2 }.map { it.question.prompt }
        assertEquals(listOf("film", "mecz", "spektakl"), twoFrames)
        assertTrue(screen.items.all { item -> item.options.size == 7 && item.question.answers.all { it in item.options } })
    }

    @Test
    fun `each row of 4J has its own recording and picture choice, without a transcript`() {
        val screen = screen("4J") as LessonScreen.Choice
        assertEquals(listOf("wb23", "wb24", "wb25", "wb26"), screen.items.map { it.track?.id })
        assertEquals(null, screen.transcript)
        assertTrue(screen.tracks.isEmpty())
    }

    @Test
    fun `the text-message rewrites count every Polish letter`() {
        listOf("5D", "5F").forEach { id ->
            val screen = screen(id) as LessonScreen.Write
            assertTrue(id, screen.questions.all { it.match == AnswerMatch.EXACT_LETTERS })
            screen.questions.forEach { assertNotEquals(id, it.prompt, it.answers.single()) }
        }
    }

    @Test
    fun `the weekend ads are matched with no person on the ad of the same number`() {
        val screen = screen("5A") as LessonScreen.Choice
        screen.items.forEachIndexed { index, item -> assertNotEquals(item.options[index], item.question.answers.single()) }
    }
}
