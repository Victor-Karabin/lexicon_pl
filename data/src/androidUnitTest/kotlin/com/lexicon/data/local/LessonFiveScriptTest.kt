package com.lexicon.data.local

import com.lexicon.model.course.LessonScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonFiveScriptTest {
    private val asset = lessonJson.decodeFromString<LessonScriptAsset>(File("src/androidMain/assets/lesson_krok-a1-05.json").readText())
    private val script = asset.toModel(asset.tracks.associate { it.file to "remote-${it.id}" })

    private fun screen(id: String) = script.screens.first { it.id == id }

    private fun lata(n: Int) = n % 10 in 2..4 && n % 100 !in 12..14

    @Test
    fun `lesson 5 has 39 screens, the grammar screen and seven steps, the last one a review`() {
        assertEquals(39, script.screens.size)
        assertEquals(listOf("G", "1", "2", "3", "4", "5", "6", "7"), script.steps.map { it.id })
        assertEquals(listOf("7A", "7B", "7C", "7D", "7E"), script.steps.single { it.isReview }.screenIds)
        assertEquals(6, script.rules.size)
        assertEquals(29, script.ruleLinks.size)
        assertTrue(script.screens.flatMap { it.tracks }.all { it.remoteId != null })
    }

    @Test
    fun `graded screens hold the number of items the plan asks for`() {
        val counts = script.screens.filter { it.isGraded }.associate { it.id to it.questions.size }
        val expected = mapOf(
            "1A" to 7, "1B" to 11, "1C" to 8, "1D" to 10,
            "2A" to 8, "2B" to 16, "2C" to 13, "2D" to 10, "2E" to 10, "2F" to 4,
            "3B" to 19, "3C" to 14, "3D" to 6, "3E" to 5,
            "4A" to 17, "4B" to 17, "4C" to 11, "4D" to 10, "4E" to 4,
            "5A" to 6, "5C" to 10, "5D" to 8, "5E" to 10,
            "6A" to 5, "6B" to 5, "6C" to 5, "6D" to 9, "6E" to 9, "6F" to 13,
            "7A" to 11, "7B" to 12, "7C" to 10, "7D" to 25, "7E" to 13,
        )
        assertEquals(expected, counts)
    }

    @Test
    fun `the bucket sorts keep rok, lata, lat in that order and put every number in its bucket`() {
        listOf("3B", "3C").forEach { id ->
            val sort = screen(id) as LessonScreen.Choice
            assertFalse(id, sort.shuffle)
            sort.items.forEach { item ->
                val number = item.question.prompt!!.toInt()
                val bucket = if (number == 1) {
                    "rok"
                } else if (lata(number)) {
                    "lata"
                } else {
                    "lat"
                }
                assertEquals("$id $number", listOf("rok", "lata", "lat"), item.options)
                assertEquals("$id $number", listOf(bucket), item.question.answers)
            }
        }
        assertEquals(
            listOf(65, 7, 57, 44, 34, 77, 22, 14, 12, 50, 20, 33, 18, 100, 13, 1, 84, 91, 63),
            (screen("3B") as LessonScreen.Choice).items.map { it.question.prompt!!.toInt() },
        )
    }

    @Test
    fun `the true-or-false screen answers P or N by ear`() {
        val truth = screen("5A") as LessonScreen.Choice
        assertEquals(listOf("P", "N"), truth.items.first().options)
        assertEquals(listOf("N", "P", "N", "N", "P", "P"), truth.items.map { it.question.answers.single() })
        assertFalse(truth.shuffle)
        assertTrue(truth.instruction.contains("P = true") && truth.instruction.contains("N = false"))
    }

    @Test
    fun `the age guesses accept exactly the numbers the word after the gap allows`() {
        val accepted = screen("3E").questions.map { question -> question.answers.map(String::toInt).toSet() }
        assertEquals(setOf(1), accepted[0])
        val lataAges = (2..120).filter(::lata).toSet()
        val latAges = (5..120).filterNot(::lata).toSet()
        assertEquals(listOf(lataAges, latAges, latAges, lataAges), accepted.drop(1))
        assertEquals(listOf("1", "82", "45", "10", "23"), screen("3E").questions.map { it.answers.first() })
        assertTrue(12 in latAges && 22 in lataAges && 101 in latAges && 104 in lataAges)
    }

    @Test
    fun `either order is fine for the paired gaps of 6A`() {
        assertEquals(listOf(listOf("1", "2"), listOf("4", "5")), screen("6A").interchangeable)
    }

    @Test
    fun `4E takes the adjective and the profession in one field`() {
        assertTrue(screen("4E").interchangeable.isEmpty())
        assertEquals("zdolna aktorka", screen("4E").questions.first().answers.single())
        assertTrue("dobry aktor" in screen("4E").questions[1].answers)
    }

    @Test
    fun `a choice item can accept both endings`() {
        val endings = screen("2C") as LessonScreen.Choice
        assertEquals(listOf("-naście", "-dzieści"), endings.items[4].question.answers)
    }

    @Test
    fun `questions accept the version without the pronoun`() {
        assertTrue(screen("1A").questions[2].answers.containsAll(listOf("Gdzie on mieszka", "Gdzie mieszka")))
        assertTrue(screen("7A").questions[1].answers.containsAll(listOf("Jak ona się nazywa?", "Jak się nazywa?")))
    }

    @Test
    fun `the answer-free free writing has no model`() {
        assertTrue((screen("1F") as LessonScreen.FreeWriting).model.isEmpty())
        assertEquals(2, (screen("1E") as LessonScreen.FreeWriting).model.size)
    }
}
