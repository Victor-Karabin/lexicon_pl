package com.lexicon.application.dictation

import com.lexicon.application.training.FakeSessionStore
import com.lexicon.interactors.dictation.SubmitDictationAnswerRequest
import com.lexicon.model.training.StepOutcome
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitDictationPunctuationTest {
    private val useCase =
        SubmitDictationAnswerUseCaseImpl(
            recordAnswer = mockk(relaxed = true),
            answerNormalizer = AnswerNormalizer(),
            sessions = FakeSessionStore(),
        )

    private suspend fun outcome(
        expected: String,
        typed: String,
    ) = useCase(
        SubmitDictationAnswerRequest(
            sessionId = "session-1",
            stepIndex = 0,
            vocabularyItemId = 1L,
            expectedText = expected,
            submittedText = typed,
            tipUsed = false,
            skipped = false,
        ),
    ).outcome

    @Test
    fun `a question typed without its question mark is correct`() =
        runTest { assertEquals(StepOutcome.CORRECT, outcome(expected = "a ty?", typed = "a ty")) }

    @Test
    fun `a phrase typed without its comma and full stop is correct`() =
        runTest { assertEquals(StepOutcome.CORRECT, outcome(expected = "Nie, dziękuję.", typed = "nie dziękuję")) }

    @Test
    fun `punctuation typed where the word has none is correct`() =
        runTest { assertEquals(StepOutcome.CORRECT, outcome(expected = "kot", typed = "kot!")) }

    @Test
    fun `an apostrophe or hyphen does not hide a wrong letter`() =
        runTest { assertEquals(StepOutcome.INCORRECT, outcome(expected = "po-polsku", typed = "po-polska")) }

    @Test
    fun `Polish letters still have to be typed with their accents`() =
        runTest { assertEquals(StepOutcome.INCORRECT, outcome(expected = "dziękuję", typed = "dziekuje")) }

    @Test
    fun `a missing word is still wrong`() =
        runTest { assertEquals(StepOutcome.INCORRECT, outcome(expected = "dzień dobry!", typed = "dzień")) }
}
