package com.lexicon.data.remote.sentence

import com.lexicon.boundary.WritingReviewRequestBoundary
import com.lexicon.boundary.WritingReviewResultBoundary
import com.lexicon.boundary.WrittenAnswerBoundary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingReviewPromptTest {
    @Test
    fun `the prompt carries the task, every answer, the criteria and the model`() {
        val prompt = writingReviewPrompt(
            WritingReviewRequestBoundary(
                task = "Write two short dialogues.",
                answers = listOf(WrittenAnswerBoundary("Formal", "Dzień dobry."), WrittenAnswerBoundary("Informal", "")),
                model = listOf("— Cześć!"),
                criteria = listOf("Informal = ty."),
            ),
        )
        listOf("Write two short dialogues.", "### Formal\nDzień dobry.", "### Informal\n(empty)", "* Informal = ty.", "— Cześć!")
            .forEach { assertTrue(it, it in prompt) }
    }

    @Test
    fun `a reply wrapped in prose or a code fence still parses`() {
        val review =
            parseWritingReview(
                "Here you go:\n```json\n{\"strengths\": [\"Good greeting.\"], \"improvements\": [\" Use *pani*. \", \"\"]}\n```",
            )
        assertEquals(listOf("Good greeting."), review?.strengths)
        assertEquals(listOf("Use *pani*."), review?.improvements)
    }

    @Test
    fun `a reply that is not a review is refused rather than shown`() {
        assertNull(parseWritingReview("Sorry, I can't help with that."))
        assertNull(parseWritingReview("{\"strengths\": [], \"improvements\": []}"))
        assertTrue(OpenAiAnswer.Text("no json").toReviewResult() is WritingReviewResultBoundary.Refused)
        assertEquals(WritingReviewResultBoundary.Offline, OpenAiAnswer.Offline.toReviewResult())
    }
}
