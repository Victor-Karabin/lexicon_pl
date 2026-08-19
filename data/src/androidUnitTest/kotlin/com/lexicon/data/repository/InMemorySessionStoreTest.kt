package com.lexicon.data.repository

import com.lexicon.model.training.Session
import com.lexicon.model.training.SessionId
import com.lexicon.model.training.Step
import com.lexicon.model.training.TrainingType
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InMemorySessionStoreTest {
    private val store = InMemorySessionStore()

    private fun session(id: String) =
        Session(
            id = SessionId(id),
            training = TrainingType.DICTATION,
            steps = listOf(Step.Question(0, VocabularyId(1), "woda")).toImmutableList(),
        )

    @Test
    fun `a saved session can be found again`() =
        runTest {
            store.save(session("a"))
            assertEquals(SessionId("a"), store.find(SessionId("a"))?.id)
        }

    @Test
    fun `saving the same session again replaces it rather than growing the store`() =
        runTest {
            store.save(session("a"))
            store.save(session("a").answer(0, com.lexicon.model.training.StepOutcome.CORRECT))

            assertEquals(1, store.find(SessionId("a"))?.correctCount)
        }

    @Test
    fun `a removed session is gone`() =
        runTest {
            store.save(session("a"))
            store.remove(SessionId("a"))
            assertNull(store.find(SessionId("a")))
        }

    /** Abandoned sessions are never removed by anyone, so the store has to bound itself. */
    @Test
    fun `the oldest session is dropped once the store is full`() =
        runTest {
            repeat(12) { store.save(session("s$it")) }

            assertNull("the first session should have aged out", store.find(SessionId("s0")))
            assertNull(store.find(SessionId("s3")))
            assertNotNull("the most recent are kept", store.find(SessionId("s11")))
            assertNotNull(store.find(SessionId("s4")))
        }

    @Test
    fun `re-saving a session keeps it from ageing out`() =
        runTest {
            store.save(session("keep"))
            repeat(7) { store.save(session("s$it")) }
            store.save(session("keep"))
            repeat(7) { store.save(session("t$it")) }

            assertNotNull(store.find(SessionId("keep")))
        }
}
