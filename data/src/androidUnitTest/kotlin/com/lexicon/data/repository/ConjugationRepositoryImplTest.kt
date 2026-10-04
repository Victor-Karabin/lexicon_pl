package com.lexicon.data.repository

import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.data.local.AssetReader
import com.lexicon.data.local.ConjugationAssetLoader
import com.lexicon.data.local.ConjugationDao
import com.lexicon.data.local.ConjugationVerbEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConjugationRepositoryImplTest {
    private val asset =
        """
        [
          {"bezokolicznik": "mieć", "ja": "mam"},
          {"bezokolicznik": "być", "ja": "jestem"}
        ]
        """.trimIndent()

    private val assets = mockk<AssetReader> { every { readText("conjugations.json") } returns asset }
    private val dao: ConjugationDao = mockk(relaxed = true)

    private val repository = ConjugationRepositoryImpl(ConjugationAssetLoader(assets), dao, mockk(relaxed = true), mockk(relaxed = true))

    private val userVerb = VerbConjugationBoundary("kotkować", mapOf("ja" to listOf("kotkuję")), "to kitten")

    @Test
    fun `a deleted bundled verb is noticed even when the user has added verbs of their own`() =
        runTest {
            coEvery { dao.countVerbs() } returns 2
            coEvery { dao.countBundledVerbs() } returns 1

            assertTrue(repository.hasDeletedVerbs())
        }

    @Test
    fun `nothing counts as deleted while every bundled verb is still there`() =
        runTest {
            coEvery { dao.countBundledVerbs() } returns 2

            assertFalse(repository.hasDeletedVerbs())
        }

    @Test
    fun `a verb written for a user's word is filed as theirs`() =
        runTest {
            val saved = slot<List<ConjugationVerbEntity>>()
            coEvery { dao.verb("kotkować") } returns null
            coEvery { dao.saveVerbs(capture(saved)) } returns Unit

            repository.saveUserVerb(userVerb)

            assertEquals("kotkować", saved.captured.single().infinitive)
            assertTrue(saved.captured.single().isUserCreated)
        }

    @Test
    fun `a bundled verb is never overwritten by one written for a user's word`() =
        runTest {
            coEvery { dao.verb("mieć") } returns ConjugationVerbEntity("mieć", "to have", "{}", isUserCreated = false)

            repository.saveUserVerb(userVerb.copy(infinitive = "mieć"))

            coVerify(exactly = 0) { dao.saveVerbs(any()) }
        }

    @Test
    fun `a user's verb can be written again`() =
        runTest {
            coEvery { dao.verb("kotkować") } returns ConjugationVerbEntity("kotkować", "", "{}", isUserCreated = true)

            repository.saveUserVerb(userVerb)

            coVerify { dao.saveVerbs(any()) }
        }
}
