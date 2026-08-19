package com.lexicon.domain.sync

import com.lexicon.boundary.CatalogSyncGate
import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.CourseRepository
import com.lexicon.boundary.SyncOutcomeBoundary
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.boundary.wasAlreadyCurrent
import com.lexicon.interactors.sync.CatalogSyncStatus
import com.lexicon.interactors.sync.SyncStepStatus
import com.lexicon.interactors.sync.isBlocked
import com.lexicon.interactors.sync.isFinished
import com.lexicon.interactors.sync.wasAlreadyCurrent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCatalogUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk(relaxed = true)
    private val presetRepository: VocabularyPresetRepository = mockk(relaxed = true)
    private val courseRepository: CourseRepository = mockk(relaxed = true)
    private val conjugationRepository: ConjugationRepository = mockk(relaxed = true)
    private val gate: CatalogSyncGate = mockk(relaxed = true) {
        coEvery { isCurrent() } returns false
    }
    private val useCase =
        SyncCatalogUseCaseImpl(
            vocabularyRepository,
            presetRepository,
            courseRepository,
            conjugationRepository,
            gate,
        )

    private fun outcome(
        total: Int,
        added: Int = 0,
    ) = SyncOutcomeBoundary(total = total, added = added, updated = 0, removed = 0)

    private suspend fun run(): List<CatalogSyncStatus> = useCase().toList()

    @Test
    fun `both steps are reported complete with their counts`() =
        runTest {
            coEvery { vocabularyRepository.syncFromSource() } returns outcome(2219, added = 2219)
            coEvery { presetRepository.syncFromSource() } returns outcome(72, added = 72)

            val final = run().last()

            assertEquals(SyncStepStatus.Complete(2219, 2219, 0, 0), final.vocabulary)
            assertEquals(SyncStepStatus.Complete(72, 72, 0, 0), final.presets)
            assertTrue(final.isFinished)
            assertFalse(final.isBlocked)
        }

    /** A launch with nothing new to load must not read a single asset. */
    @Test
    fun `an unchanged app version skips every step`() =
        runTest {
            coEvery { gate.isCurrent() } returns true
            coEvery { vocabularyRepository.countWords() } returns 2563

            val final = run().last()

            assertTrue(final.isFinished)
            assertEquals(SyncStepStatus.Complete(2563, 0, 0, 0), final.vocabulary)
            coVerify(exactly = 0) { vocabularyRepository.syncFromSource() }
            coVerify(exactly = 0) { conjugationRepository.syncFromSource() }
        }

    @Test
    fun `a completed sync records the version so the next launch can skip it`() =
        runTest {
            coEvery { vocabularyRepository.syncFromSource() } returns outcome(10)
            coEvery { presetRepository.syncFromSource() } returns outcome(3)
            coEvery { courseRepository.syncFromSource() } returns outcome(26)
            coEvery { conjugationRepository.syncFromSource() } returns outcome(4545)

            run()

            coVerify { gate.markCurrent() }
        }

    @Test
    fun `a blocked sync is not recorded, so it runs again next launch`() =
        runTest {
            coEvery { vocabularyRepository.syncFromSource() } throws IllegalStateException("no asset")
            coEvery { vocabularyRepository.countWords() } returns 0

            run()

            coVerify(exactly = 0) { gate.markCurrent() }
        }

    @Test
    fun `the verbs are seeded as their own step`() =
        runTest {
            coEvery { vocabularyRepository.syncFromSource() } returns outcome(10)
            coEvery { presetRepository.syncFromSource() } returns outcome(3)
            coEvery { courseRepository.syncFromSource() } returns outcome(26)
            coEvery { conjugationRepository.syncFromSource() } returns outcome(4545, added = 4545)

            val emissions = run()

            assertTrue(emissions.any { it.verbs is SyncStepStatus.InProgress })
            assertEquals(SyncStepStatus.Complete(4545, 4545, 0, 0), emissions.last().verbs)
            assertTrue(emissions.last().isFinished)
        }

    @Test
    fun `a failed vocabulary skips the verbs too`() =
        runTest {
            coEvery { vocabularyRepository.syncFromSource() } throws IllegalStateException("no asset")
            coEvery { vocabularyRepository.countWords() } returns 0

            val final = run().last()

            assertTrue(final.verbs is SyncStepStatus.Failed)
            assertTrue(final.isBlocked)
        }

    @Test
    fun `each step is reported in progress before it completes`() =
        runTest {
            coEvery { vocabularyRepository.syncFromSource() } returns outcome(10)
            coEvery { presetRepository.syncFromSource() } returns outcome(3)

            val emissions = run()

            assertTrue(emissions.any { it.vocabulary is SyncStepStatus.InProgress })
            assertTrue(emissions.any { it.presets is SyncStepStatus.InProgress })
        }

    @Test
    fun `an unchanged catalogue is reported as already current`() =
        runTest {
            coEvery { vocabularyRepository.syncFromSource() } returns outcome(2219)
            coEvery { presetRepository.syncFromSource() } returns outcome(72)

            val vocabulary = run().last().vocabulary as SyncStepStatus.Complete

            assertTrue(vocabulary.wasAlreadyCurrent)
        }

    @Test
    fun `presets are skipped when the vocabulary could not be loaded at all`() =
        runTest {
            coEvery { vocabularyRepository.syncFromSource() } throws IllegalStateException("asset missing")
            coEvery { vocabularyRepository.countWords() } returns 0

            val final = run().last()

            assertTrue(final.isBlocked)
            assertTrue(final.presets is SyncStepStatus.Failed)
            coVerify(exactly = 0) { presetRepository.syncFromSource() }
        }

    @Test
    fun `a failed vocabulary sync still lets the app continue when words are already stored`() =
        runTest {
            coEvery { vocabularyRepository.syncFromSource() } throws IllegalStateException("asset missing")
            coEvery { vocabularyRepository.countWords() } returns 2219
            coEvery { presetRepository.syncFromSource() } returns outcome(72)

            val final = run().last()

            assertFalse("stale words are still usable", final.isBlocked)
            coVerify { presetRepository.syncFromSource() }
        }

    @Test
    fun `the failure reason is carried through to the screen`() =
        runTest {
            coEvery { vocabularyRepository.syncFromSource() } throws IllegalStateException("asset missing")
            coEvery { vocabularyRepository.countWords() } returns 0

            val failure = run().last().vocabulary as SyncStepStatus.Failed

            assertEquals("asset missing", failure.reason)
        }

    @Test
    fun `a failed preset sync over a stored catalogue does not block`() =
        runTest {
            coEvery { vocabularyRepository.syncFromSource() } returns outcome(2219)
            coEvery { presetRepository.syncFromSource() } throws IllegalStateException("preset asset missing")
            coEvery { presetRepository.getPresets() } returns listOf(mockk())

            val final = run().last()

            assertTrue(final.isFinished)
            assertFalse(final.isBlocked)
        }

    @Test
    fun `an unfinished sync is not reported as finished`() =
        runTest {
            coEvery { vocabularyRepository.syncFromSource() } returns outcome(1)
            coEvery { presetRepository.syncFromSource() } returns outcome(1)

            val emissions = run()

            assertFalse("the first emission cannot be finished", emissions.first().isFinished)
            assertTrue(emissions.last().isFinished)
        }
}
