package com.lexicon.data.local

import com.lexicon.model.course.AnswerMatch
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.allTracks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonNineScriptTest {
    private val asset = lessonJson.decodeFromString<LessonScriptAsset>(File("src/androidMain/assets/lesson_krok-a1-09.json").readText())
    private val script = asset.toModel(asset.tracks.associate { it.file to "remote-${it.id}" })

    private fun screen(id: String) = script.screens.first { it.id == id }

    @Test
    fun `lesson 9 has 30 screens, the grammar screen and six steps, the last one a review`() {
        assertEquals(30, script.screens.size)
        assertEquals(listOf("G", "1", "2", "3", "4", "5", "6"), script.steps.map { it.id })
        assertEquals(listOf("6A"), script.steps.single { it.isReview }.screenIds)
        assertEquals(5, script.rules.size)
        assertEquals(21, script.ruleLinks.size)
        assertTrue(script.screens.flatMap { it.allTracks() }.all { it.remoteId != null })
    }

    @Test
    fun `graded screens hold the number of items the plan asks for`() {
        val counts = script.screens.filter { it.isGraded }.associate { it.id to it.questions.size }
        val expected = mapOf(
            "1B" to 12, "1C" to 13, "1D" to 16, "1E" to 4, "1F" to 9, "1G" to 8, "1I" to 24,
            "2A" to 12, "2B" to 18, "2C" to 13, "2D" to 10, "2E" to 6,
            "3B" to 21, "3C" to 41, "3D" to 12, "3E" to 10, "3F" to 14, "3G" to 11,
            "4A" to 7, "4B" to 9,
            "5A" to 4, "5B" to 43, "5C" to 12, "5D" to 23, "5E" to 6, "6A" to 32,
        )
        assertEquals(expected, counts)
    }

    @Test
    fun `each market dialogue asks the questions and then the gaps under one player`() {
        mapOf("2A" to (5 to 7), "2B" to (8 to 10), "2C" to (6 to 7)).forEach { (id, sizes) ->
            val gapFill = screen(id) as LessonScreen.GapFill
            assertEquals(id, sizes, gapFill.choices.size to gapFill.gaps.size)
            assertEquals(id, 1, gapFill.tracks.size)
            assertEquals(id, gapFill.choices.map { it.question } + gapFill.gaps, gapFill.questions)
        }
    }

    @Test
    fun `the plural endings keep the traps`() {
        val endings = (screen("3C") as LessonScreen.Choice).items.associate { it.question.prompt to it.question.answers.single() }
        assertEquals("-e", endings["kukurydza"])
        assertEquals("-e", endings["fasola"])
        assertEquals("-i", endings["pieróg"])
        assertEquals("-y", endings["lód"])
        assertEquals(listOf("-y", "-i", "-e", "-a"), (screen("3C") as LessonScreen.Choice).items.first().options)
    }

    @Test
    fun `the odd word is picked and the reason is written`() {
        val oddOne = screen("5C") as LessonScreen.GapFill
        assertEquals(listOf("kurczak", "por", "ser", "arbuz", "bułka", "wino"), oddOne.choices.map { it.question.answers.single() })
        assertEquals(listOf("mięso", "warzywo", "nabiał", "owoc", "pieczywo", "alkohol"), oddOne.gaps.map { it.answers.first() })
    }

    @Test
    fun `the four basket recordings play on one screen`() {
        val baskets = screen("5A") as LessonScreen.Choice
        assertEquals(listOf("109E1-1", "109E1-2", "109E1-3", "109E1-4"), baskets.tracks.map { it.id })
        assertEquals(listOf("1", "4", "3", "2"), baskets.items.map { it.question.answers.single() })
    }

    @Test
    fun `salads are described with the ingredients in any order`() {
        assertTrue((screen("1E") as LessonScreen.Write).questions.all { it.match == AnswerMatch.ANY_ORDER })
    }

    @Test
    fun `the anagrams use exactly the letters of their word`() {
        (screen("1C") as LessonScreen.Write).questions.forEach {
            assertEquals(it.answers.single(), it.prompt!!.toList().sorted(), it.answers.single().toList().sorted())
        }
    }
}
