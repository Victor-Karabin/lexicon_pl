package com.lexicon.presentation.common

import app.cash.turbine.test
import com.lexicon.common.DispatcherProvider
import com.lexicon.interactors.settings.GetStepCountUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StepSessionTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers =
        object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

    private val results = LastSessionResultsHolder()
    private val getStepCount: GetStepCountUseCase = mockk { coEvery { this@mockk() } returns STEPS }
    private val queue = ArrayDeque(listOf("kot", "pies", "dom", "las"))
    private val askedWhenPicking = mutableListOf<Set<String>>()

    private fun TestScope.session(pick: suspend (Set<String>) -> String? = { asked -> pickFromQueue(asked) }) =
        StepSession(
            scope = this,
            dispatchers = dispatchers,
            speechSynthesizer = mockk(relaxed = true),
            resultsHolder = results,
            recordStudy = mockk(relaxed = true),
            getStepCount = getStepCount,
            keyOf = { it },
            pick = pick,
        )

    private fun pickFromQueue(asked: Set<String>): String? {
        askedWhenPicking += asked.toSet()
        return queue.removeFirstOrNull()
    }

    @Test
    fun `begin reads the session length and hands over the first item`() =
        runTest(testDispatcher) {
            val session = session()
            var first: String? = "unset"

            session.begin { first = it }
            advanceUntilIdle()

            assertEquals("kot", first)
            assertEquals(STEPS, session.totalSteps)
        }

    @Test
    fun `begin waits for a suspending handler and still hands over the first item`() =
        runTest(testDispatcher) {
            val session = session()
            var first: String? = "unset"

            session.begin { value ->
                delay(1)
                first = value
            }
            advanceUntilIdle()

            assertEquals("kot", first)
        }

    @Test
    fun `begin hands over null when there is nothing to practise`() =
        runTest(testDispatcher) {
            val session = session(pick = { null })
            var first: String? = "unset"

            session.begin { first = it }
            advanceUntilIdle()

            assertNull(first)
            assertEquals(STEPS, session.totalSteps)
        }

    @Test
    fun `an item already shown is passed to the next pick as asked`() =
        runTest(testDispatcher) {
            val session = session()
            session.begin { }
            advanceUntilIdle()

            session.advance(stepIndex = 0) { _, _ -> }
            advanceUntilIdle()

            assertEquals(listOf(emptySet(), setOf("kot")), askedWhenPicking)
        }

    @Test
    fun `advance shows the next item at the next index`() =
        runTest(testDispatcher) {
            val session = session()
            session.begin { }
            advanceUntilIdle()
            val shown = mutableListOf<Pair<String, Int>>()

            session.advance(stepIndex = 0) { item, index -> shown += item to index }
            advanceUntilIdle()

            assertEquals(listOf("pies" to 1), shown)
        }

    @Test
    fun `a second advance before the first has finished is ignored`() =
        runTest(testDispatcher) {
            val session = session()
            session.begin { }
            advanceUntilIdle()
            val shown = mutableListOf<Pair<String, Int>>()

            session.advance(stepIndex = 0) { item, index -> shown += item to index }
            session.advance(stepIndex = 0) { item, index -> shown += item to index }
            advanceUntilIdle()

            assertEquals(listOf("pies" to 1), shown)
        }

    @Test
    fun `beforeMove runs once for a double advance`() =
        runTest(testDispatcher) {
            val session = session()
            session.begin { }
            advanceUntilIdle()
            var moves = 0

            session.advance(stepIndex = 0, beforeMove = { moves++ }) { _, _ -> }
            session.advance(stepIndex = 0, beforeMove = { moves++ }) { _, _ -> }
            advanceUntilIdle()

            assertEquals(1, moves)
        }

    @Test
    fun `advancing is possible again once the step has been shown`() =
        runTest(testDispatcher) {
            val session = session()
            session.begin { }
            advanceUntilIdle()
            val shown = mutableListOf<Pair<String, Int>>()

            session.advance(stepIndex = 0) { item, index -> shown += item to index }
            advanceUntilIdle()
            session.advance(stepIndex = 1) { item, index -> shown += item to index }
            advanceUntilIdle()

            assertEquals(listOf("pies" to 1, "dom" to 2), shown)
        }

    @Test
    fun `after the last step the tally and the word results are handed on`() =
        runTest(testDispatcher) {
            val session = session()
            session.begin { }
            advanceUntilIdle()
            val right = WordResultEntry("kot", "cat", AnswerState.Correct)
            val wrong = WordResultEntry("pies", "dog", AnswerState.Incorrect("pies"))
            session.record(right)
            session.record(wrong)
            session.record(wrong)

            session.navigationEvents.test {
                session.advance(stepIndex = STEPS - 1) { _, _ -> }
                advanceUntilIdle()

                assertEquals(SessionNavigationEvent.SessionComplete(correct = 1, incorrect = 2, skipped = 0, tipsUsed = 0), awaitItem())
            }
            assertEquals(listOf(right, wrong, wrong), results.wordResults)
        }

    @Test
    fun `running out of items ends the session before the last step`() =
        runTest(testDispatcher) {
            val session = session(pick = { asked -> if (asked.isEmpty()) "kot" else null })
            session.begin { }
            advanceUntilIdle()
            session.record(WordResultEntry("kot", "cat", AnswerState.Correct))

            session.navigationEvents.test {
                session.advance(stepIndex = 0) { _, _ -> }
                advanceUntilIdle()

                assertEquals(SessionNavigationEvent.SessionComplete(correct = 1, incorrect = 0, skipped = 0, tipsUsed = 0), awaitItem())
            }
        }

    @Test
    fun `studied marks study time through the use case`() =
        runTest(testDispatcher) {
            val recordStudy = mockk<com.lexicon.interactors.training.RecordStudyActivityUseCase>(relaxed = true)
            val session = StepSession(
                scope = this,
                dispatchers = dispatchers,
                speechSynthesizer = mockk(relaxed = true),
                resultsHolder = results,
                recordStudy = recordStudy,
                getStepCount = getStepCount,
                keyOf = { it: String -> it },
                pick = { null },
            )

            session.studied()
            advanceUntilIdle()

            coVerify(exactly = 1) { recordStudy() }
        }

    private companion object {
        const val STEPS = 3
    }
}
