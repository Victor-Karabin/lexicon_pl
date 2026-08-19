package com.lexicon.domain.presets

import com.lexicon.boundary.VocabularyItemBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.CefrLevel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchVocabularyUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk()
    private val useCase = SearchVocabularyUseCaseImpl(vocabularyRepository)

    private fun repositoryReturns(vararg items: VocabularyItemBoundary) {
        coEvery { vocabularyRepository.search(any(), any(), any()) } returns items.toList()
    }

    @Test
    fun `results are mapped into words, level included`() =
        runTest {
            repositoryReturns(VocabularyItemBoundary(1L, "woda", "water", "ˈvɔda", isInStudySet = true, cefr = "A1"))

            val word = useCase("woda").single()

            assertEquals("woda", word.text)
            assertEquals("water", word.translation)
            assertEquals(CefrLevel.A1, word.cefr)
            assertTrue("the study-set flag has to survive the mapping", word.isInStudySet)
        }

    @Test
    fun `a level the app does not know degrades to null rather than failing`() =
        runTest {
            repositoryReturns(VocabularyItemBoundary(1L, "woda", "water", "ˈvɔda", cefr = "D3"))

            assertNull(useCase("woda").single().cefr)
        }

    @Test
    fun `the query reaches the repository folded`() =
        runTest {
            val sent = slot<String>()
            coEvery { vocabularyRepository.search(capture(sent), any(), any()) } returns emptyList()

            useCase("ŻÓŁW")

            assertEquals("zolw", sent.captured)
        }

    @Test
    fun `case and surrounding spaces do not change the query`() =
        runTest {
            val sent = slot<String>()
            coEvery { vocabularyRepository.search(capture(sent), any(), any()) } returns emptyList()

            useCase("  Apple  ")

            assertEquals("apple", sent.captured)
        }

    @Test
    fun `levels alone are a search, with an empty query`() =
        runTest {
            val query = slot<String>()
            val levels = slot<Set<String>>()
            coEvery { vocabularyRepository.search(capture(query), capture(levels), any()) } returns emptyList()

            useCase(levels = setOf(CefrLevel.A1, CefrLevel.A2))

            assertEquals("", query.captured)
            assertEquals(setOf("A1", "A2"), levels.captured)
        }

    @Test
    fun `a query and levels narrow together`() =
        runTest {
            val query = slot<String>()
            val levels = slot<Set<String>>()
            coEvery { vocabularyRepository.search(capture(query), capture(levels), any()) } returns emptyList()

            useCase("wod", setOf(CefrLevel.B1))

            assertEquals("wod", query.captured)
            assertEquals(setOf("B1"), levels.captured)
        }

    @Test
    fun `no query and no levels returns nothing and never reaches the repository`() =
        runTest {
            assertTrue(useCase().isEmpty())
            coVerify(exactly = 0) { vocabularyRepository.search(any(), any(), any()) }
        }

    @Test
    fun `a query of only spaces is treated as empty`() =
        runTest {
            assertTrue(useCase("   ").isEmpty())
            coVerify(exactly = 0) { vocabularyRepository.search(any(), any(), any()) }
        }

    @Test
    fun `the default limit is above the size of the vocabulary`() =
        runTest {
            val limit = slot<Int>()
            coEvery { vocabularyRepository.search(any(), any(), capture(limit)) } returns emptyList()

            useCase(levels = setOf(CefrLevel.A1))

            assertTrue("a level must not be truncated, got ${limit.captured}", limit.captured >= 2_000)
        }
}
