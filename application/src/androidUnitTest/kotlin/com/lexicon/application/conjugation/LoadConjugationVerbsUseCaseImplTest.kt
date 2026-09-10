package com.lexicon.application.conjugation

import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.VerbConjugationBoundary
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val PAGES_A_WALK_MAY_TAKE = 50

class LoadConjugationVerbsUseCaseImplTest {
    private fun teachable(index: Int) = VerbConjugationBoundary("verb$index", mapOf("ja" to listOf("form$index")))

    private fun unteachable(index: Int) = VerbConjugationBoundary("broken$index", emptyMap())

    private fun useCaseOver(table: List<VerbConjugationBoundary>): LoadConjugationVerbsUseCaseImpl {
        val repository: ConjugationRepository = mockk {
            coEvery { verbPage(any(), any(), any()) } answers {
                val limit = secondArg<Int>()
                val offset = thirdArg<Int>()
                table.drop(offset).take(limit)
            }
        }
        return LoadConjugationVerbsUseCaseImpl(repository)
    }

    private suspend fun LoadConjugationVerbsUseCaseImpl.walk(): List<String> {
        val seen = mutableListOf<String>()
        var offset = 0
        repeat(PAGES_A_WALK_MAY_TAKE) {
            val page = page(query = "", offset = offset)
            seen += page.verbs.map { it.infinitive }
            if (page.isLast) return seen
            offset = page.nextOffset
        }
        error("paging never reached the end of the table; got ${seen.size} verbs")
    }

    @Test
    fun `paging past unteachable verbs reaches every teachable verb exactly once`() =
        runTest {
            val table = (0 until 30).map(::teachable) + (30 until 80).map(::unteachable) + (80 until 130).map(::teachable)

            val seen = useCaseOver(table).walk()

            val expected = table.filter { it.forms.isNotEmpty() }.map { it.infinitive }
            assertEquals(expected, seen)
        }

    @Test
    fun `a table that ends on a page boundary still reports its last page`() =
        runTest {
            val table = (0 until 80).map(::teachable)

            val seen = useCaseOver(table).walk()

            assertEquals(80, seen.size)
        }

    @Test
    fun `a query matching nothing is the last page straight away`() =
        runTest {
            val page = useCaseOver(emptyList()).page(query = "zzz", offset = 0)

            assertTrue(page.verbs.isEmpty())
            assertTrue(page.isLast)
        }
}
