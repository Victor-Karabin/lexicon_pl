package com.lexicon.data.local

import com.lexicon.model.course.AnswerMatch
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.allTracks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonSevenScriptTest {
    private val missing = setOf("107LOTTO", "107B5", "107B7")
    private val asset = lessonJson.decodeFromString<LessonScriptAsset>(File("src/androidMain/assets/lesson_krok-a1-07.json").readText())
    private val script = asset.toModel(asset.tracks.associate { it.file to "remote-${it.id}".takeUnless { _ -> it.id in missing } })

    private fun screen(id: String) = script.screens.first { it.id == id }

    @Test
    fun `lesson 7 has 37 screens, the grammar screen and six steps, the last one a review`() {
        assertEquals(37, script.screens.size)
        assertEquals(listOf("G", "1", "2", "3", "4", "5", "6"), script.steps.map { it.id })
        assertEquals(listOf("6A"), script.steps.single { it.isReview }.screenIds)
        assertEquals(5, script.rules.size)
        assertEquals(29, script.ruleLinks.size)
    }

    @Test
    fun `only the three recordings still to come from the CD have no download`() {
        val withoutDownload = script.screens.flatMap { it.allTracks() }.filter { it.remoteId == null }.map { it.id }.toSet()
        assertEquals(missing, withoutDownload)
    }

    @Test
    fun `graded screens hold the number of items the plan asks for`() {
        val counts = script.screens.filter { it.isGraded }.associate { it.id to it.questions.size }
        val expected = mapOf(
            "1A" to 4, "1C" to 20, "1D" to 10, "1E" to 8, "1F" to 10, "1G" to 10,
            "2B" to 6, "2C" to 6, "2D" to 16, "2E" to 7, "2F" to 9, "2G" to 8,
            "3B" to 6, "3C" to 6, "3D" to 10, "3E" to 10, "3F" to 10, "3G" to 8, "3H" to 5,
            "4A" to 19, "4B" to 10, "4C" to 11, "4D" to 5, "4E" to 4, "4F" to 11, "4G" to 4, "4H" to 7,
            "5A" to 11, "5B" to 14, "6A" to 11,
        )
        assertEquals(expected, counts)
    }

    @Test
    fun `each Lotto coupon is a grid of 1 to 49 in rows of seven with six numbers to find`() {
        listOf("3B", "3C").forEach { id ->
            val grid = screen(id) as LessonScreen.NumberGrid
            assertEquals(49, grid.size)
            assertEquals(7, grid.columns)
            assertTrue(grid.questions.all { it.match == AnswerMatch.NUMBER_SET && it.answers.single().split(" ").size == 6 })
        }
    }

    @Test
    fun `the verb pairs use each word of the box once`() {
        val pairs = screen("1C") as LessonScreen.GapFill
        assertTrue(pairs.eachAnswerOnce)
        assertEquals(pairs.wordBox.toSet(), pairs.questions.map { it.answers.first() }.toSet())
    }

    @Test
    fun `the washing machine may go to the laundry or to household appliances`() {
        assertEquals(listOf("18", "19"), (screen("4A") as LessonScreen.Write).questions[8].answers)
    }

    @Test
    fun `answers about the text count when they hold the key words`() {
        assertTrue((screen("4D") as LessonScreen.Write).questions.all { it.match == AnswerMatch.KEY_WORDS })
    }
}
