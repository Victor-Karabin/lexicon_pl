package com.lexicon.presentation.mix

import com.lexicon.interactors.mix.MixStep
import com.lexicon.interactors.puzzle.PuzzleStepResponse
import com.lexicon.interactors.trueorfalse.TrueOrFalseStepResponse
import com.lexicon.presentation.common.AnswerState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MixUiStateTest {
    private fun trueOrFalseState(
        answerState: AnswerState = AnswerState.Unanswered,
        isSubmitting: Boolean = false,
        answeredTrue: Boolean? = null,
    ) = MixUiState.Loaded(
        step = MixStep.TrueOrFalse(0, TrueOrFalseStepResponse(0, 1L, "chleb", "bread", true)),
        answerState = answerState,
        isSubmitting = isSubmitting,
        answeredTrue = answeredTrue,
    )

    private fun puzzleState(
        answerState: AnswerState = AnswerState.Unanswered,
        isSubmitting: Boolean = false,
    ) = MixUiState.Loaded(
        step = MixStep.Puzzle(0, PuzzleStepResponse(0, 1L, "praca", null, "work")),
        answerState = answerState,
        isSubmitting = isSubmitting,
    )

    /**
     * Regression: an incorrect answer left isSubmitting set, and awaitingNext is gated on it, so
     * Next never appeared and every other control was disabled — the step soft-locked.
     */
    @Test
    fun `an incorrect step offers Next once submission has settled`() {
        val state = puzzleState(answerState = AnswerState.Incorrect("praca"), isSubmitting = false)

        assertTrue("Next must be offered, or the step has no way forward", state.awaitingNext)
    }

    @Test
    fun `an incorrect True or False step also offers Next`() {
        val state = trueOrFalseState(answerState = AnswerState.Incorrect(), isSubmitting = false)

        assertTrue(state.awaitingNext)
    }

    @Test
    fun `Next is withheld only while a submission is still in flight`() {
        assertFalse(puzzleState(answerState = AnswerState.Incorrect("praca"), isSubmitting = true).awaitingNext)
    }

    @Test
    fun `True or False has no Check action, since it answers on tap`() {
        assertFalse(trueOrFalseState().hasCheckAction)
        assertFalse("Check could never enable, so it must not be shown", trueOrFalseState().canCheck)
    }

    @Test
    fun `the other step types do have a Check action`() {
        assertTrue(puzzleState().hasCheckAction)
    }

    @Test
    fun `True or False inherits its standalone lack of Tip and Skip`() {
        val state = trueOrFalseState()

        assertFalse(state.canUseTip)
        assertFalse(state.canSkip)
    }

    @Test
    fun `Skip stays available on step types that support it`() {
        assertTrue(puzzleState().canSkip)
    }

    @Test
    fun `a True or False step reveals no expected answer, because the choice was the answer`() {
        val state = trueOrFalseState(answerState = AnswerState.Incorrect())

        assertNull(state.revealedAnswer)
    }

    @Test
    fun `step types with a spelling answer still reveal it when wrong`() {
        assertTrue(puzzleState(answerState = AnswerState.Incorrect("praca")).revealedAnswer == "praca")
    }

    @Test
    fun `both True or False buttons start with no outcome colour`() {
        val state = trueOrFalseState()

        assertNull(state.trueOrFalseOutcomeFor(isTrueButton = true))
        assertNull(state.trueOrFalseOutcomeFor(isTrueButton = false))
    }

    @Test
    fun `a correct answer colours only the button that was tapped`() {
        val state = trueOrFalseState(answerState = AnswerState.Correct, answeredTrue = true)

        assertTrue(state.trueOrFalseOutcomeFor(isTrueButton = true) == true)
        assertNull("the untapped button stays neutral", state.trueOrFalseOutcomeFor(isTrueButton = false))
    }

    @Test
    fun `an incorrect answer colours the tapped button as incorrect`() {
        val state = trueOrFalseState(answerState = AnswerState.Incorrect(), answeredTrue = false)

        assertTrue(state.trueOrFalseOutcomeFor(isTrueButton = false) == false)
        assertNull(state.trueOrFalseOutcomeFor(isTrueButton = true))
    }
}
