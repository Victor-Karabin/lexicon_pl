package com.lexicon.domain.course

import com.lexicon.boundary.ExerciseItemBoundary
import com.lexicon.boundary.LessonExerciseBoundary
import com.lexicon.interactors.course.LessonExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CourseMappersTest {
    private fun boundary(
        type: String,
        items: List<ExerciseItemBoundary>,
    ) = LessonExerciseBoundary(
        id = "lesson-1-101a1",
        type = type,
        instruction = "Proszę powtórzyć",
        audioFile = "101a1.mp3",
        items = items,
    )

    @Test
    fun `repeat exercise reads each item's prompt as a word to say back`() {
        val exercise = boundary(
            type = "repeat",
            items = listOf(
                ExerciseItemBoundary(label = null, prompt = "kot", options = emptyList(), answers = emptyList()),
                ExerciseItemBoundary(label = null, prompt = "pies", options = emptyList(), answers = emptyList()),
            ),
        ).toExercise() as LessonExercise.Repeat

        assertEquals(listOf("kot", "pies"), exercise.words)
    }

    @Test
    fun `repeat exercise drops items with no prompt`() {
        val exercise = boundary(
            type = "repeat",
            items = listOf(
                ExerciseItemBoundary(label = null, prompt = "kot", options = emptyList(), answers = emptyList()),
                ExerciseItemBoundary(label = null, prompt = null, options = emptyList(), answers = emptyList()),
            ),
        ).toExercise() as LessonExercise.Repeat

        assertEquals(listOf("kot"), exercise.words)
    }

    @Test
    fun `minimal pair exercise keeps a well-formed item`() {
        val exercise = boundary(
            type = "minimal_pair",
            items = listOf(
                ExerciseItemBoundary(label = "a", prompt = null, options = listOf("kot", "kod"), answers = listOf("kot")),
            ),
        ).toExercise() as LessonExercise.MinimalPair

        val item = exercise.items.single()
        assertEquals("a", item.label)
        assertEquals(listOf("kot", "kod"), item.options)
        assertEquals("kot", item.answer)
    }

    @Test
    fun `minimal pair exercise drops an item that does not have exactly two options`() {
        val exercise = boundary(
            type = "minimal_pair",
            items = listOf(
                ExerciseItemBoundary(label = "a", prompt = null, options = listOf("kot"), answers = listOf("kot")),
                ExerciseItemBoundary(
                    label = "b",
                    prompt = null,
                    options = listOf("pies", "kod", "but"),
                    answers = listOf("pies"),
                ),
            ),
        ).toExercise() as LessonExercise.MinimalPair

        assertEquals(emptyList<Any>(), exercise.items)
    }

    @Test
    fun `minimal pair exercise drops an item with no recognised answer`() {
        val exercise = boundary(
            type = "minimal_pair",
            items = listOf(
                ExerciseItemBoundary(label = "a", prompt = null, options = listOf("kot", "kod"), answers = emptyList()),
            ),
        ).toExercise() as LessonExercise.MinimalPair

        assertEquals(emptyList<Any>(), exercise.items)
    }

    @Test
    fun `gap fill exercise keeps a well-formed item`() {
        val exercise = boundary(
            type = "gap_fill",
            items = listOf(
                ExerciseItemBoundary(
                    label = null,
                    prompt = "Dzień dobry, ___ się Mami",
                    options = emptyList(),
                    answers = listOf("Nazywam"),
                ),
            ),
        ).toExercise() as LessonExercise.GapFill

        val item = exercise.items.single()
        assertEquals("Dzień dobry, ___ się Mami", item.prompt)
        assertEquals(listOf("Nazywam"), item.answers)
    }

    @Test
    fun `gap fill exercise drops an item with no prompt`() {
        val exercise = boundary(
            type = "gap_fill",
            items = listOf(
                ExerciseItemBoundary(label = null, prompt = null, options = emptyList(), answers = listOf("Nazywam")),
            ),
        ).toExercise() as LessonExercise.GapFill

        assertEquals(emptyList<Any>(), exercise.items)
    }

    @Test
    fun `gap fill exercise keeps a line with nothing missing, which a dialogue opens with`() {
        val exercise = boundary(
            type = "gap_fill",
            items = listOf(
                ExerciseItemBoundary(
                    label = "Sekretarka",
                    prompt = "Dzień dobry. Jestem Agnieszka Polańska.",
                    options = emptyList(),
                    answers = emptyList(),
                ),
            ),
        ).toExercise() as LessonExercise.GapFill

        val item = exercise.items.single()
        assertEquals("Sekretarka", item.speaker)
        assertEquals(emptyList<Any>(), item.answers)
    }

    @Test
    fun `letter fill exercise drops a word with more gaps than letters to fill them`() {
        val exercise = boundary(
            type = "letter_fill",
            items = listOf(
                ExerciseItemBoundary(label = "a", prompt = "j_d_n", options = emptyList(), answers = listOf("e")),
                ExerciseItemBoundary(label = "b", prompt = "_wa", options = emptyList(), answers = listOf("d")),
            ),
        ).toExercise() as LessonExercise.LetterFill

        assertEquals("dwa", exercise.items.single().answer)
    }

    @Test
    fun `a match prompt naming a drawing reads as an icon rather than as words`() {
        val exercise = boundary(
            type = "match",
            items = listOf(
                ExerciseItemBoundary(
                    label = "1",
                    prompt = "icon:repeat",
                    options = emptyList(),
                    answers = listOf("proszę powtórzyć"),
                ),
                ExerciseItemBoundary(
                    label = "2",
                    prompt = "Co to znaczy „dom”?",
                    options = emptyList(),
                    answers = listOf("Nie wiem."),
                ),
            ),
        ).toExercise() as LessonExercise.Match

        assertEquals("repeat", exercise.items.first().iconName)
        assertNull(exercise.items.last().iconName)
    }

    @Test
    fun `an exercise type this build does not know is dropped rather than rendered blank`() {
        val exercise = boundary(type = "crossword", items = emptyList()).toExercise()

        assertNull(exercise)
    }
}
