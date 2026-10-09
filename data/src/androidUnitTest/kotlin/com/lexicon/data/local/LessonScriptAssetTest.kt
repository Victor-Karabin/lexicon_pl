package com.lexicon.data.local

import com.lexicon.model.course.AnswerKeyboard
import com.lexicon.model.course.LessonProgress
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.TranscriptUnlock
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
    fun `lesson 1 has eleven screens in three steps that cover every screen once`() {
        assertEquals(listOf("1A", "1B", "1C", "2A", "2B", "2C", "3A", "3B", "3C", "3D", "3E"), script.screens.map { it.id })
        assertEquals(script.screens.map { it.id }, script.steps.flatMap { it.screenIds })
        assertEquals(0.8, script.passMark, 0.0)
    }

    @Test
    fun `graded screens hold the number of items the coursebook asks for`() {
        val counts = script.screens.filter { it.isGraded }.associate { it.id to it.questions.size }
        assertEquals(mapOf("1B" to 8, "1C" to 22, "2B" to 8, "3B" to 5, "3D" to 8, "3E" to 6), counts)
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
    fun `every gap in a dialogue has an answer and every answer a gap`() {
        script.screens.filterIsInstance<LessonScreen.GapFill>().forEach { gaps ->
            val inText = gaps.sections.flatMap { it.lines }.flatMap { line ->
                com.lexicon.model.course.GAP_PATTERN.findAll(line.text).map { it.groupValues[1] }.toList()
            }
            assertEquals(gaps.id, gaps.questions.map { it.key }, inText)
        }
    }

    @Test
    fun `every choice answer is one of its options`() {
        (screen("1C") as LessonScreen.Choice).items.forEach { item ->
            assertTrue(item.question.label, item.question.answers.single() in item.options)
        }
    }

    @Test
    fun `tracks resolve to files and their download ids`() {
        val dictation = screen("1B").tracks.single()
        assertEquals("a1_coursebook_101b3.mp3", dictation.file)
        assertEquals("remote-101b3", dictation.remoteId)
        assertEquals(2, screen("2A").tracks.size)
        assertTrue(screen("3E").tracks.isEmpty())
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
            finished = persistentSetOf("1A"),
        )
        val stored = lessonJson.encodeToString(LessonProgressAsset.serializer(), progress.toAsset())
        assertEquals(progress, lessonJson.decodeFromString<LessonProgressAsset>(stored).toModel())
    }
}
