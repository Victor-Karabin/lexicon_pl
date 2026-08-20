package com.lexicon.data.local

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlBatchingTest {
    @Test
    fun `a list that fits is passed through in one go`() =
        runTest {
            val calls = mutableListOf<Int>()
            val ids = (1L..10L).toList()

            val result = ids.inBatches { batch ->
                calls += batch.size
                batch
            }

            assertEquals(listOf(10), calls)
            assertEquals(ids, result)
        }

    @Test
    fun `a list too long for one statement is cut into batches`() =
        runTest {
            val calls = mutableListOf<Int>()
            val ids = (1L..2_000L).toList()

            val result = ids.inBatches { batch ->
                calls += batch.size
                batch
            }

            assertTrue("no batch may exceed the limit", calls.all { it <= MAX_SQL_VARIABLES })
            assertEquals("nothing may be dropped", ids, result)
            assertEquals(2_000, calls.sum())
        }

    @Test
    fun `the limit stays under what Android 8 allows`() {
        assertTrue("SQLite there refuses more than 999 host parameters", MAX_SQL_VARIABLES < 999)
    }

    @Test
    fun `a statement over a long list runs once per batch`() =
        runTest {
            val seen = mutableListOf<Long>()

            (1L..1_500L).toList().forEachBatch { batch -> seen += batch }

            assertEquals(1_500, seen.size)
            assertEquals((1L..1_500L).toList(), seen)
        }

    @Test
    fun `an empty list asks nothing of the database`() =
        runTest {
            var called = 0
            val result = emptyList<Long>().inBatches {
                called++
                it
            }

            assertEquals(1, called)
            assertTrue(result.isEmpty())
        }
}
