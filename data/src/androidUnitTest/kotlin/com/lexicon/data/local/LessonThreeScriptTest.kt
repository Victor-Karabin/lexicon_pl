package com.lexicon.data.local

import com.lexicon.model.course.ItemPicture
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.TranscriptUnlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonThreeScriptTest {
    private val asset = lessonJson.decodeFromString<LessonScriptAsset>(File("src/androidMain/assets/lesson_krok-a1-03.json").readText())
    private val script = asset.toModel(emptyMap())

    private fun screen(id: String) = script.screens.first { it.id == id }

    @Test
    fun `lesson 3 has 34 screens in eight steps, the last one a review`() {
        assertEquals(34, script.screens.size)
        assertEquals(8, script.steps.size)
        assertEquals(listOf("8A", "8B"), script.steps.single { it.isReview }.screenIds)
    }

    @Test
    fun `graded screens hold the number of items the plan asks for`() {
        val counts = script.screens.filter { it.isGraded }.associate { it.id to it.questions.size }
        val expected = mapOf(
            "1B" to 16, "1C" to 8, "1D" to 9, "1E" to 8, "1F" to 4,
            "2A" to 13, "2B" to 10, "2C" to 6, "2D" to 11,
            "3B" to 14, "3C" to 7, "3D" to 8, "3E" to 9,
            "4B" to 17, "4C" to 26,
            "5B" to 10, "5C" to 12, "5D" to 20, "5E" to 3,
            "6A" to 9, "6B" to 8, "6C" to 7,
            "7A" to 11, "7B" to 10, "7C" to 10, "7D" to 8, "7E" to 6, "7F" to 16,
            "8A" to 8, "8B" to 8,
        )
        assertEquals(expected, counts)
    }

    @Test
    fun `the ten-ta-to screen asks both choices inside one sentence`() {
        val inline = screen("5D") as LessonScreen.InlineChoice
        val first = inline.lines.first()
        assertEquals("[1] klucz jest [2]", first.text)
        assertEquals(listOf(listOf("Ten", "Ta", "To"), listOf("nowy", "nowa", "nowe")), first.groups.map { it.options })
        assertEquals(listOf("Ten", "nowy"), first.groups.map { it.question.answers.single() })
    }

    @Test
    fun `picture items show an icon or a colour swatch`() {
        val bag = screen("2A").questions.first().picture as ItemPicture.Symbol
        assertEquals("👜" to "bag", bag.emoji to bag.label)
        assertEquals("table", (screen("2A").questions[2].picture as ItemPicture.Symbol).icon)
        assertEquals(ItemPicture.Swatch(0xFFD62828), screen("7A").questions.first().picture)
        assertTrue(screen("7C").questions.all { it.picture is ItemPicture.Swatch })
    }

    @Test
    fun `adjectives of one gender may go with any noun of that gender, each once`() {
        val pairs = screen("6B") as LessonScreen.GapFill
        assertEquals(listOf(listOf("1", "4", "7"), listOf("2", "6"), listOf("3", "5", "8")), pairs.interchangeable)
        assertEquals(8, pairs.wordBox.size)
    }

    @Test
    fun `the yes-or-no answers accept the optional second sentence, and the no answers of 3C require it`() {
        val chair = screen("3B").questions[2]
        assertEquals(listOf("Nie, to nie jest klucz.", "Nie, to nie jest klucz. To jest krzesło."), chair.answers)
        assertEquals(listOf("Tak, to jest CD płyta."), screen("3B").questions[6].variants)
        assertEquals(listOf("Nie, to nie jest okno. To jest klucz."), screen("3C").questions[0].answers)
    }

    @Test
    fun `missing recordings keep their task and point at the expected file`() {
        assertEquals("a1_coursebook_103a4-2.mp3", screen("1D").tracks.single().file)
        assertEquals("a1_coursebook_103b1.mp3", screen("3A").tracks.single().file)
        assertEquals("a1_coursebook_103d1.mp3", screen("7A").tracks.single().file)
        assertTrue(screen("1D") is LessonScreen.GapFill)
    }

    @Test
    fun `classroom words open after the dictation is checked`() {
        assertEquals(TranscriptUnlock.AfterCheck, script.newWords.getValue("1B").unlock)
        assertEquals(13, script.newWords.getValue("1B").words.size)
        assertNull(screen("2A").hint)
    }
}
