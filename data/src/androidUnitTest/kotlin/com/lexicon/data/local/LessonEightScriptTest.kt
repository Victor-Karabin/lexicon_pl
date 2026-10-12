package com.lexicon.data.local

import com.lexicon.model.course.AnswerMatch
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.allTracks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonEightScriptTest {
    private val asset = lessonJson.decodeFromString<LessonScriptAsset>(File("src/androidMain/assets/lesson_krok-a1-08.json").readText())
    private val script = asset.toModel(asset.tracks.associate { it.file to "remote-${it.id}" })

    private fun screen(id: String) = script.screens.first { it.id == id }

    @Test
    fun `lesson 8 has 38 screens, the grammar screen and six steps, the last one a review`() {
        assertEquals(38, script.screens.size)
        assertEquals(listOf("G", "1", "2", "3", "4", "5", "6"), script.steps.map { it.id })
        assertEquals(listOf("6A"), script.steps.single { it.isReview }.screenIds)
        assertEquals(5, script.rules.size)
        assertEquals(24, script.ruleLinks.size)
        assertTrue(script.screens.flatMap { it.allTracks() }.all { it.remoteId != null })
    }

    @Test
    fun `graded screens hold the number of items the plan asks for`() {
        val counts = script.screens.filter { it.isGraded }.associate { it.id to it.questions.size }
        val expected = mapOf(
            "1C" to 16, "1D" to 8, "1E" to 9, "1F" to 6, "1G" to 14, "1H" to 18, "1I" to 7,
            "2B" to 8, "2C" to 21, "2D" to 8, "2E" to 7, "2F" to 10, "2G" to 7,
            "3A" to 3, "3B" to 4, "3C" to 12, "3D" to 11, "3E" to 15, "3F" to 4,
            "4A" to 13, "4C" to 4, "4E" to 9, "4F" to 12, "4G" to 15, "4H" to 10,
            "5A" to 6, "5B" to 7, "5C" to 8, "5D" to 17, "5E" to 17, "5F" to 24, "6A" to 15,
        )
        assertEquals(expected, counts)
    }

    @Test
    fun `each person picks the letters A to H for what they eat and drink`() {
        val grid = screen("3B") as LessonScreen.SelectionGrid
        assertEquals(listOf("A", "B", "C", "D", "E", "F", "G", "H"), grid.options)
        assertEquals(8, grid.legend.size)
        assertEquals(listOf("C G", "E", "B H", "A D F"), grid.questions.map { it.answers.single() })
    }

    @Test
    fun `phrases are built with the ingredients in any order`() {
        listOf("2B", "2F", "3F").forEach { id ->
            assertTrue(id, (screen(id) as LessonScreen.Write).questions.all { it.match == AnswerMatch.ANY_ORDER })
        }
    }

    @Test
    fun `the breakfast pairs may swap within the same food`() {
        assertEquals(listOf(listOf("1", "2", "3"), listOf("4", "5"), listOf("6", "7")), screen("1D").interchangeable)
    }

    @Test
    fun `fish and chips count as a product and as a dish`() {
        val items = (screen("6A") as LessonScreen.Choice).items.associate { it.question.prompt to it.question.answers.toSet() }
        assertEquals(setOf("produkt", "potrawa"), items["ryba"])
        assertEquals(setOf("produkt", "potrawa"), items["frytki"])
    }
}
