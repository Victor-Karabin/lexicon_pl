package com.lexicon.presentation.common

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTallyTest {
    private val lastResults = LastSessionResultsHolder()
    private val tally = SessionTally(lastResults)

    @Test
    fun `each outcome is counted, tips separately, and the result screen gets the totals`() =
        runTest(UnconfinedTestDispatcher()) {
            val event = async { tally.events.first() }

            tally.record(AnswerState.Correct, "kot", "cat")
            tally.record(AnswerState.Correct, "pies", "dog", tipUsed = true)
            tally.record(AnswerState.Incorrect("dom"), "dom", "house")
            tally.record(AnswerState.Skipped("las"), "las", "forest")
            tally.countTip()
            tally.complete()

            assertEquals(SessionNavigationEvent.SessionComplete(correct = 2, incorrect = 1, skipped = 1, tipsUsed = 1), event.await())
            assertEquals(
                listOf("kot", "pies", "dom", "las"),
                lastResults.wordResults.map { it.word },
            )
            assertTrue(lastResults.wordResults[1].tipUsed)
        }

    @Test
    fun `a board counted without words leaves no word results behind`() =
        runTest(UnconfinedTestDispatcher()) {
            lastResults.wordResults = listOf(WordResultEntry("stale", "old", AnswerState.Correct))
            val event = async { tally.events.first() }

            tally.record(AnswerState.Correct)
            tally.record(AnswerState.Skipped())
            tally.complete()

            assertEquals(SessionNavigationEvent.SessionComplete(correct = 1, incorrect = 0, skipped = 1), event.await())
            assertTrue(lastResults.wordResults.isEmpty())
        }
}
