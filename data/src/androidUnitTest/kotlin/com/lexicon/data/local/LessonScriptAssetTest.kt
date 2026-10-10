package com.lexicon.data.local

import com.lexicon.model.course.AnswerKeyboard
import com.lexicon.model.course.LessonProgress
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.TranscriptUnlock
import com.lexicon.model.course.WritingReview
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LessonScriptAssetTest {
    private val asset = lessonJson.decodeFromString<LessonScriptAsset>(File("src/androidMain/assets/lesson_krok-a1-01.json").readText())
    private val script = asset.toModel(asset.tracks.associate { it.file to "remote-${it.id}" })

    private fun screen(id: String) = script.screens.first { it.id == id }

    @Test
    fun `lesson 1 has 27 screens in seven steps that cover every screen once`() {
        assertEquals(27, script.screens.size)
        assertEquals(7, script.steps.size)
        assertEquals(script.screens.map { it.id }, script.steps.flatMap { it.screenIds })
        assertEquals(listOf("4A", "4B", "4C", "4D"), script.steps[3].screenIds)
    }

    @Test
    fun `graded screens hold the number of items the coursebook asks for`() {
        val counts = script.screens.filter { it.isGraded }.associate { it.id to it.questions.size }
        val expected = mapOf(
            "1B" to 8, "1C" to 22, "2B" to 8, "3B" to 5, "3D" to 8, "3E" to 6,
            "4B" to 4, "4C" to 4, "4D" to 5,
            "5B" to 6, "5C" to 10, "5D" to 5,
            "6B" to 6, "6C" to 5, "6D" to 4,
            "7A" to 4, "7B" to 7, "7C" to 16, "7D" to 19,
        )
        assertEquals(expected, counts)
    }

    @Test
    fun `screen types match the lesson plan`() {
        assertTrue(screen("1A") is LessonScreen.Reference)
        assertTrue(screen("1C") is LessonScreen.Choice)
        assertTrue(screen("2B") is LessonScreen.GapFill)
        assertTrue(screen("2C") is LessonScreen.FreeWriting)
        assertEquals(AnswerKeyboard.DIGITS, (screen("3D") as LessonScreen.Write).keyboard)
    }

    @Test
    fun `matching screens list each reply once and use each reply once`() {
        listOf("5B", "5D").forEach { id ->
            val matching = screen(id) as LessonScreen.Choice
            val answers = matching.items.map { it.question.answers.single() }
            assertEquals(id, answers.toSet().size, answers.size)
            assertEquals(id, matching.items.first().options.size, matching.legend.size)
        }
    }

    @Test
    fun `word banks and first digits are hints, hidden behind their own control`() {
        listOf("5C", "6B", "6D").forEach { assertTrue(it, !screen(it).hint.isNullOrBlank()) }
        assertTrue((screen("4C") as LessonScreen.Write).questions.all { it.prompt != null })
        assertTrue((screen("7D") as LessonScreen.Write).questions.all { it.prompt != null })
    }

    @Test
    fun `numbering starts at 1 and no item is an example`() {
        listOf("7B", "7C", "7D").forEach { id ->
            val labels = screen(id).questions.map { it.label }
            assertEquals(id, (1..labels.size).map(Int::toString), labels)
        }
    }

    @Test
    fun `tracks resolve to files and their download ids`() {
        val dictation = screen("1B").tracks.single()
        assertEquals("a1_coursebook_101b3.mp3", dictation.file)
        assertEquals("remote-101b3", dictation.remoteId)
        assertTrue(screen("3E").tracks.isEmpty())
        assertEquals("a1_workbook_08_L01_cwiczenie16.mp3", screen("7D").tracks.single().file)
    }

    @Test
    fun `each dialogue sits next to its own recording`() {
        val listen = screen("2A") as LessonScreen.Reference
        assertTrue(listen.tracks.isEmpty())
        assertEquals(
            listOf("a1_coursebook_101a1.mp3", "a1_coursebook_101a2.mp3", "a1_coursebook_101A5.mp3"),
            listen.groups.mapNotNull { it.track?.file },
        )
        assertTrue(listen.groups.all { it.phrases.isNotEmpty() })

        val fill = screen("2B") as LessonScreen.GapFill
        assertTrue(fill.tracks.isEmpty())
        assertEquals(listOf("a1_coursebook_101a1.mp3", "a1_coursebook_101a2.mp3"), fill.sections.map { it.track?.file })
    }

    @Test
    fun `the dialogue transcript stays hidden until the gap fill is checked`() {
        assertEquals(TranscriptUnlock.AfterScreen("2B"), screen("2A").transcript?.unlock)
        assertEquals(TranscriptUnlock.AfterCheck, screen("1B").transcript?.unlock)
    }

    @Test
    fun `progress survives a round trip through its stored form`() {
        val progress = LessonProgress(
            screenIndex = 4,
            answers = persistentMapOf("2B" to persistentMapOf("1" to "dobry")),
            checked = persistentSetOf("1B"),
            finished = persistentSetOf("1A", "2C"),
            reviews = persistentMapOf("2C" to WritingReview(persistentListOf("Good greeting."), persistentListOf("Use *pani*."))),
        )
        val stored = lessonJson.encodeToString(LessonProgressAsset.serializer(), progress.toAsset())
        assertEquals(progress, lessonJson.decodeFromString<LessonProgressAsset>(stored).toModel())
    }

    @Test
    fun `a track can carry its own download id when the course has none for it`() {
        val withId = asset.copy(tracks = asset.tracks.map { if (it.id == "wb08") it.copy(remoteId = "drive-wb08") else it })
        val cities = withId.toModel(emptyMap()).screens.first { it.id == "7D" }.tracks.single()
        assertEquals("drive-wb08", cities.remoteId)
        val fromCourse = withId.toModel(mapOf(cities.file to "course-wb08")).screens.first { it.id == "7D" }.tracks.single()
        assertEquals("course-wb08", fromCourse.remoteId)
    }
}
