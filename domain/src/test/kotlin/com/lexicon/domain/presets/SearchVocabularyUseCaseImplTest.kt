package com.lexicon.domain.presets

import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchVocabularyUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk()
    private val useCase = SearchVocabularyUseCaseImpl(vocabularyRepository)

    private fun repositoryReturns(vararg items: VocabularyItemBoundary) {
        coEvery { vocabularyRepository.search(any(), any()) } returns items.toList()
    }

    @Test
    fun `results are mapped into words`() =
        runTest {
            repositoryReturns(VocabularyItemBoundary(1L, "woda", "water", "ˈvɔda", isFavourite = true))

            val results = useCase("woda")

            assertEquals(listOf("woda"), results.map { it.text })
            assertEquals(listOf("water"), results.map { it.translation })
            assertTrue("the favourite flag has to survive the mapping", results.single().isFavourite)
        }

    /**
     * The stored keys are folded, so the query must be folded by the same rule. Sending the
     * raw query is how a search silently stops matching anything with a diacritic in it.
     */
    @Test
    fun `the query reaches the repository folded`() =
        runTest {
            val sent = slot<String>()
            coEvery { vocabularyRepository.search(capture(sent), any()) } returns emptyList()

            useCase("ŻÓŁW")

            assertEquals("zolw", sent.captured)
        }

    @Test
    fun `case and surrounding spaces do not change the query`() =
        runTest {
            val sent = slot<String>()
            coEvery { vocabularyRepository.search(capture(sent), any()) } returns emptyList()

            useCase("  Apple  ")

            assertEquals("apple", sent.captured)
        }

    /** An empty search box means "nothing typed yet", not "return the whole vocabulary". */
    @Test
    fun `an empty query returns nothing and never reaches the repository`() =
        runTest {
            val results = useCase("")

            assertTrue(results.isEmpty())
            coVerify(exactly = 0) { vocabularyRepository.search(any(), any()) }
        }

    @Test
    fun `a query of only spaces is treated as empty`() =
        runTest {
            assertTrue(useCase("   ").isEmpty())
            coVerify(exactly = 0) { vocabularyRepository.search(any(), any()) }
        }

    @Test
    fun `the limit is passed through so a broad query cannot return everything`() =
        runTest {
            val limit = slot<Int>()
            coEvery { vocabularyRepository.search(any(), capture(limit)) } returns emptyList()

            useCase("a", limit = 25)

            assertEquals(25, limit.captured)
        }
}
