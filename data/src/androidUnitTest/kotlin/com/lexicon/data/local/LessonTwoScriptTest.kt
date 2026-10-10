package com.lexicon.data.local

import com.lexicon.model.course.AnswerKeyboard
import com.lexicon.model.course.AnswerMatch
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.TranscriptUnlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonTwoScriptTest {
    private val asset = lessonJson.decodeFromString<LessonScriptAsset>(File("src/androidMain/assets/lesson_krok-a1-02.json").readText())
    private val script = asset.toModel(asset.tracks.associate { it.file to "remote-${it.id}" })

    private fun screen(id: String) = script.screens.first { it.id == id }

    @Test
    fun `lesson 2 has 40 screens in eight steps, the last one a review`() {
        assertEquals(40, script.screens.size)
        assertEquals(listOf("7A", "7B", "7C", "7D", "7E", "7F", "7G"), script.steps[6].screenIds)
        assertEquals(listOf("8"), script.steps.filter { it.isReview }.map { it.id })
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8"), script.steps.map { it.id })
        assertEquals(listOf("2A", "2B", "2C", "2D", "2E", "2F"), script.steps[1].screenIds)
    }

    @Test
    fun `graded screens hold the number of items the plan asks for`() {
        val counts = script.screens.filter { it.isGraded }.associate { it.id to it.questions.size }
        val expected = mapOf(
            "1B" to 18, "1C" to 12,
            "2C" to 7, "2D" to 6, "2E" to 6, "2F" to 6,
            "3A" to 6, "3B" to 19, "3C" to 6, "3D" to 14, "3E" to 10, "3F" to 7,
            "4B" to 6, "4C" to 15,
            "5B" to 6, "5C" to 6, "5D" to 9, "5E" to 10,
            "6A" to 12, "6B" to 10, "6C" to 10, "6E" to 9,
            "7B" to 6, "7C" to 6, "7D" to 6, "7E" to 6, "7F" to 6, "7G" to 5,
            "8A" to 8, "8B" to 12, "8C" to 10, "8D" to 7, "8E" to 12,
        )
        assertEquals(expected, counts)
    }

    @Test
    fun `screen types match the lesson plan`() {
        listOf("1A", "2A", "4A", "5A", "6D", "7A").forEach { assertTrue(it, screen(it) is LessonScreen.Reference) }
        listOf("2E", "8B").forEach { assertTrue(it, screen(it) is LessonScreen.Ordering) }
        assertTrue(screen("2B") is LessonScreen.FreeWriting)
        listOf("7C", "7D", "7E", "7F").forEach { assertTrue(it, screen(it) is LessonScreen.Form) }
        assertEquals(AnswerKeyboard.DIGITS, (screen("6B") as LessonScreen.Write).keyboard)
    }

    @Test
    fun `each questionnaire is one person with one recording and accepts short answers`() {
        val people = mapOf("7C" to "Uwe", "7D" to "Manuela", "7E" to "Maria", "7F" to "Tom")
        val recordings = mapOf("7C" to "102f1", "7D" to "102f1", "7E" to "102f1-2", "7F" to "102f1-2")
        people.forEach { (id, person) ->
            val form = screen(id) as LessonScreen.Form
            assertEquals(id, listOf(person), form.groups.map { it.title })
            assertEquals(id, 6, form.questions.size)
            assertTrue(id, form.questions.all { it.match == AnswerMatch.KEY_WORDS })
            assertEquals(id, listOf("a1_coursebook_${recordings.getValue(id)}.mp3"), form.tracks.map { it.file })
            assertTrue(id, form.questions.first().variants.isNotEmpty())
            assertEquals(id, TranscriptUnlock.AfterCheck, form.transcript?.unlock)
        }
    }

    @Test
    fun `the scale screen lists the words shuffled against a fixed mark scale`() {
        val scale = screen("8D") as LessonScreen.Choice
        val marks = listOf("+++", "++", "+", "+/−", "−", "−−", "−−−")
        assertTrue(scale.items.all { it.options == marks })
        assertTrue(!scale.shuffle)
        assertEquals("tak sobie", scale.items.first().question.prompt)
    }

    @Test
    fun `the formal questions accept pan as well, and the less usual orders show the usual one`() {
        val questions = screen("8E").questions
        assertTrue("Jak się pan nazywa?" in questions[2].answers)
        assertTrue("Jak pani się nazywa?" in questions[2].variants)
        assertTrue("Jakie ma pani imię?" in questions[0].variants)
    }

    @Test
    fun `the two co items of the matching screen are checked as a set`() {
        val halves = screen("2D") as LessonScreen.Choice
        assertEquals(listOf(listOf("0", "2")), halves.interchangeable)
        assertEquals(listOf("co …", "co …"), listOf(halves.items[0], halves.items[2]).map { it.question.prompt })
    }

    @Test
    fun `two-word gaps accept both word orders`() {
        val gaps = screen("5C").questions
        assertTrue(gaps.all { gap -> gap.answers.size == 2 && gap.answers.map { it.lowercase().split(' ').toSet() }.toSet().size == 1 })
    }

    @Test
    fun `the 7A dialogues open only once the last questionnaire is checked`() {
        assertEquals(TranscriptUnlock.AfterScreen("7F"), screen("7A").transcript?.unlock)
        assertEquals(TranscriptUnlock.AfterCheck, screen("2E").transcript?.unlock)
    }

    @Test
    fun `recordings resolve to the files the course ships, including the misnamed 0102e3`() {
        assertEquals("a1_coursebook_0102e3.mp3", screen("6B").tracks.single().file)
        assertEquals("a1_workbook_09_L02_cwiczenie2.mp3", screen("2E").tracks.single().file)
        assertEquals("a1_coursebook_102e4.mp3", screen("6D").tracks.single().file)
        assertEquals(listOf("a1_coursebook_102c1.mp3"), screen("3B").tracks.map { it.file })
        assertEquals(1, (screen("3B") as LessonScreen.GapFill).sections.size)
    }

    @Test
    fun `word banks and verb lists are hints`() {
        listOf("3A", "4B", "6A").forEach { assertTrue(it, !screen(it).hint.isNullOrBlank()) }
        assertNull(screen("3E").hint)
    }

    @Test
    fun `numbering starts at 1 and no item is an example`() {
        listOf("2C", "2D", "3C", "3D", "3E", "3F", "5E", "6C", "6E", "7B", "7G", "8C", "8D", "8E").forEach { id ->
            val labels = screen(id).questions.map { it.label }
            assertEquals(id, (1..labels.size).map(Int::toString), labels)
        }
    }
}
