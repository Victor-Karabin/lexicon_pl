package com.lexicon.data.local

import com.lexicon.model.course.GAP_PATTERN
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.TranscriptUnlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonFourScriptTest {
    private val asset = lessonJson.decodeFromString<LessonScriptAsset>(File("src/androidMain/assets/lesson_krok-a1-04.json").readText())
    private val script = asset.toModel(asset.tracks.associate { it.file to "remote-${it.id}".takeUnless { _ -> it.id == "104C7" } })

    private fun screen(id: String) = script.screens.first { it.id == id }

    @Test
    fun `lesson 4 has 32 screens, the grammar screen and six steps, the last one a review`() {
        assertEquals(32, script.screens.size)
        assertEquals(listOf("G", "1", "2", "3", "4", "5", "6"), script.steps.map { it.id })
        assertEquals(listOf("6A", "6B", "6C"), script.steps.single { it.isReview }.screenIds)
        assertEquals(5, script.rules.size)
    }

    @Test
    fun `graded screens hold the number of items the plan asks for`() {
        val counts = script.screens.filter { it.isGraded }.associate { it.id to it.questions.size }
        val expected = mapOf(
            "1A" to 8, "1B" to 8,
            "2B" to 6, "2C" to 12, "2D" to 8, "2E" to 7, "2F" to 9, "2G" to 27,
            "3B" to 24, "3C" to 11, "3D" to 16, "3E" to 11, "3F" to 6, "3G" to 6, "3H" to 6,
            "4B" to 6, "4C" to 8, "4D" to 10, "4E" to 12,
            "5C" to 7, "5D" to 8, "5E" to 5,
            "6A" to 9, "6B" to 7, "6C" to 15,
        )
        assertEquals(expected, counts)
    }

    @Test
    fun `the language table shows each language as four short lines, one given and three to type`() {
        val table = screen("2G") as LessonScreen.GapFill
        assertEquals(9, table.sections.size)
        table.sections.forEach { section ->
            assertEquals(4, section.lines.size)
            assertEquals(3, section.lines.count { GAP_PATTERN.containsMatchIn(it.text) })
        }
        assertEquals("język angielski", table.sections.first().lines.first().text)
        assertTrue(!table.instruction.contains("One form is given"))
    }

    @Test
    fun `the conjugation screen shows one verb table per verb with every form typed`() {
        val screen = screen("3B") as LessonScreen.Conjugation
        assertEquals(listOf("mówić", "lubić", "kończyć", "tańczyć"), screen.tables.map { it.verb })
        assertEquals(listOf("to speak", "to like", "to finish", "to dance"), screen.tables.map { it.translation })
        assertEquals(24, screen.questions.size)
        assertEquals(listOf("mówię"), screen.tables.first().rows.first().answers)
    }

    @Test
    fun `one sentence of 6C holds all its conjunction choices`() {
        val first = (screen("6C") as LessonScreen.InlineChoice).lines.first()
        assertEquals(3, first.groups.size)
        assertEquals(listOf("ani", "ani", "ale"), first.groups.map { it.question.answers.single() })
        assertTrue(first.groups.all { it.options == listOf("i", "ani", "ale", "bo") })
    }

    @Test
    fun `the internationalisms keep their listening task with a placeholder player`() {
        val track = screen("5B").tracks.single()
        assertEquals("a1_coursebook_104c7.mp3", track.file)
        assertNull(track.remoteId)
        assertTrue(screen("5B") is LessonScreen.Reference)
    }

    @Test
    fun `the free writing has one single-line model per person`() {
        val writing = screen("5F") as LessonScreen.FreeWriting
        assertEquals(2, writing.fields.size)
        assertEquals(listOf(false, false), writing.fields.indices.map(writing::isMultiline))
    }

    @Test
    fun `rule links follow the lesson plan`() {
        assertEquals(listOf(1, 2), script.rulesOf("3F").map { it.number })
        assertEquals(listOf(3, 5), script.rulesOf("5F").map { it.number })
        assertEquals(23, script.ruleLinks.size)
    }

    @Test
    fun `answers that are the new words open them after the check`() {
        assertEquals(TranscriptUnlock.AfterCheck, script.newWords.getValue("4C").unlock)
        assertEquals("wysoki · brudna · pracowity · gruby · smutna · młody · brzydka · chora", screen("4C").hint)
    }
}
