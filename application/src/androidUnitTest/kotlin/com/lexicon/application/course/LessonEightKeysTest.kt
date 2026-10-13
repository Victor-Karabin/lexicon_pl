package com.lexicon.application.course

import com.lexicon.interactors.course.AnswerVerdict
import com.lexicon.interactors.course.LessonAnswerChecker
import com.lexicon.model.course.AnswerMatch
import org.junit.Assert.assertEquals
import org.junit.Test

class LessonEightKeysTest {
    private val lesson = LessonAsset(8)

    private fun listed(
        id: String,
        index: Int,
        typed: String,
    ) = LessonAnswerChecker.verdict(lesson.items(id)[index].list("answers"), typed, AnswerMatch.ANY_ORDER)

    @Test
    fun `a sandwich is right with its ingredients in any order`() {
        assertEquals(AnswerVerdict.CORRECT, listed("3F", 0, "Uwe je chleb z masłem, kiełbasą, keczupem i musztardą."))
        assertEquals(AnswerVerdict.CORRECT, listed("3F", 0, "Uwe je chleb z musztardą i z keczupem, kiełbasą i masłem"))
        assertEquals(AnswerVerdict.WRONG, listed("3F", 0, "Uwe je chleb z masłem, kiełbasą i keczupem"))
        assertEquals(AnswerVerdict.CORRECT, listed("3F", 3, "Ja jem bułkę z wędliną, jajkiem i musztardą."))
    }

    @Test
    fun `phrase building takes i or z before each noun`() {
        assertEquals(AnswerVerdict.CORRECT, listed("2B", 0, "chleb z masłem i dżemem malinowym"))
        assertEquals(AnswerVerdict.CORRECT, listed("2B", 0, "chleb z masłem i z dżemem malinowym"))
        assertEquals(AnswerVerdict.ALMOST, listed("2B", 6, "chleb z bialym serem i sola"))
        assertEquals(AnswerVerdict.CORRECT, listed("2F", 4, "sok jabłkowy i jedzą ciasto malinowe"))
    }

    @Test
    fun `each person is right only with exactly their letters`() {
        val uwe = lesson.items("3B")[3].list("answers")
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(uwe, "F D A", AnswerMatch.SELECTION))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(uwe, "A D", AnswerMatch.SELECTION))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(uwe, "A D F G", AnswerMatch.SELECTION))
    }

    @Test
    fun `masculine fruit and vegetables take the animate accusative`() {
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(lesson.gap("1H", 8), "banana"))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(lesson.gap("1H", 8), "banan"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(lesson.gap("1H", 13), "pomidora"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(lesson.gap("5F", 10), "kurczaka"))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(lesson.gap("5F", 10), "kurczak"))
    }

    @Test
    fun `the instrumental plural takes -ymi and -ami`() {
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(lesson.gap("4E", 4), "kiszonymi ogórkami"))
        assertEquals(AnswerVerdict.WRONG, LessonAnswerChecker.verdict(lesson.gap("4E", 4), "kiszonym ogórkiem"))
        assertEquals(AnswerVerdict.CORRECT, LessonAnswerChecker.verdict(lesson.gap("4F", 2), "pieczonymi ziemniakami"))
        assertEquals(AnswerVerdict.ALMOST, LessonAnswerChecker.verdict(lesson.gap("4E", 4), "kiszonymi ogorkami"))
    }
}
