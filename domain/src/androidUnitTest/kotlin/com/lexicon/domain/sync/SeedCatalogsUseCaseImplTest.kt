package com.lexicon.domain.sync

import com.lexicon.boundary.CatalogSeedGate
import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.CourseRepository
import com.lexicon.boundary.SeedOutcomeBoundary
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.boundary.wasAlreadyCurrent
import com.lexicon.interactors.sync.CatalogSeedStatus
import com.lexicon.interactors.sync.SeedStepStatus
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

class SeedCatalogsUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk(relaxed = true)
    private val presetRepository: VocabularyPresetRepository = mockk(relaxed = true)
    private val courseRepository: CourseRepository = mockk(relaxed = true)
    private val conjugationRepository: ConjugationRepository = mockk(relaxed = true)
    private val gate: CatalogSeedGate = mockk(relaxed = true) {
        coEvery { isCurrent() } returns false
    }
    private val useCase =
        SeedCatalogsUseCaseImpl(
            vocabularyRepository,
            presetRepository,
            courseRepository,
            conjugationRepository,
            gate,
        )

    private fun outcome(
        total: Int,
        added: Int = 0,
    ) = SeedOutcomeBoundary(total = total, added = added, updated = 0, removed = 0)

    private suspend fun run(): List<CatalogSeedStatus> = useCase().toList()

    @Test
    fun `both steps are reported complete with their counts`() =
        runTest {
            coEvery { vocabularyRepository.seedFromAsset() } returns outcome(2219, added = 2219)
            coEvery { presetRepository.seedFromAsset() } returns outcome(72, added = 72)

            val final = run().last()

            assertEquals(SeedStepStatus.Complete(2219, 2219, 0, 0), final.vocabulary)
            assertEquals(SeedStepStatus.Complete(72, 72, 0, 0), final.presets)
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
            assertEquals(SeedStepStatus.Complete(2563, 0, 0, 0), final.vocabulary)
            coVerify(exactly = 0) { vocabularyRepository.seedFromAsset() }
            coVerify(exactly = 0) { conjugationRepository.seedFromAsset() }
        }

    @Test
    fun `a completed sync records the version so the next launch can skip it`() =
        runTest {
            coEvery { vocabularyRepository.seedFromAsset() } returns outcome(10)
            coEvery { presetRepository.seedFromAsset() } returns outcome(3)
            coEvery { courseRepository.seedFromAsset() } returns outcome(26)
            coEvery { conjugationRepository.seedFromAsset() } returns outcome(4545)

            run()

            coVerify { gate.markCurrent() }
        }

    @Test
    fun `a blocked sync is not recorded, so it runs again next launch`() =
        runTest {
            coEvery { vocabularyRepository.seedFromAsset() } throws IllegalStateException("no asset")
            coEvery { vocabularyRepository.countWords() } returns 0

            run()

            coVerify(exactly = 0) { gate.markCurrent() }
        }

    @Test
    fun `the verbs are seeded as their own step`() =
        runTest {
            coEvery { vocabularyRepository.seedFromAsset() } returns outcome(10)
            coEvery { presetRepository.seedFromAsset() } returns outcome(3)
            coEvery { courseRepository.seedFromAsset() } returns outcome(26)
            coEvery { conjugationRepository.seedFromAsset() } returns outcome(4545, added = 4545)

            val emissions = run()

            assertTrue(emissions.any { it.verbs is SeedStepStatus.InProgress })
            assertEquals(SeedStepStatus.Complete(4545, 4545, 0, 0), emissions.last().verbs)
            assertTrue(emissions.last().isFinished)
        }

    @Test
    fun `a failed vocabulary skips the verbs too`() =
        runTest {
            coEvery { vocabularyRepository.seedFromAsset() } throws IllegalStateException("no asset")
            coEvery { vocabularyRepository.countWords() } returns 0

            val final = run().last()

            assertTrue(final.verbs is SeedStepStatus.Failed)
            assertTrue(final.isBlocked)
        }

    @Test
    fun `each step is reported in progress before it completes`() =
        runTest {
            coEvery { vocabularyRepository.seedFromAsset() } returns outcome(10)
            coEvery { presetRepository.seedFromAsset() } returns outcome(3)

            val emissions = run()

            assertTrue(emissions.any { it.vocabulary is SeedStepStatus.InProgress })
            assertTrue(emissions.any { it.presets is SeedStepStatus.InProgress })
        }

    @Test
    fun `an unchanged catalogue is reported as already current`() =
        runTest {
            coEvery { vocabularyRepository.seedFromAsset() } returns outcome(2219)
            coEvery { presetRepository.seedFromAsset() } returns outcome(72)

            val vocabulary = run().last().vocabulary as SeedStepStatus.Complete

            assertTrue(vocabulary.wasAlreadyCurrent)
        }

    @Test
    fun `presets are skipped when the vocabulary could not be loaded at all`() =
        runTest {
            coEvery { vocabularyRepository.seedFromAsset() } throws IllegalStateException("asset missing")
            coEvery { vocabularyRepository.countWords() } returns 0

            val final = run().last()

            assertTrue(final.isBlocked)
            assertTrue(final.presets is SeedStepStatus.Failed)
            coVerify(exactly = 0) { presetRepository.seedFromAsset() }
        }

    @Test
    fun `a failed vocabulary sync still lets the app continue when words are already stored`() =
        runTest {
            coEvery { vocabularyRepository.seedFromAsset() } throws IllegalStateException("asset missing")
            coEvery { vocabularyRepository.countWords() } returns 2219
            coEvery { presetRepository.seedFromAsset() } returns outcome(72)

            val final = run().last()

            assertFalse("stale words are still usable", final.isBlocked)
            coVerify { presetRepository.seedFromAsset() }
        }

    @Test
    fun `the failure reason is carried through to the screen`() =
        runTest {
            coEvery { vocabularyRepository.seedFromAsset() } throws IllegalStateException("asset missing")
            coEvery { vocabularyRepository.countWords() } returns 0

            val failure = run().last().vocabulary as SeedStepStatus.Failed

            assertEquals("asset missing", failure.reason)
        }

    @Test
    fun `a failed preset sync over a stored catalogue does not block`() =
        runTest {
            coEvery { vocabularyRepository.seedFromAsset() } returns outcome(2219)
            coEvery { presetRepository.seedFromAsset() } throws IllegalStateException("preset asset missing")
            coEvery { presetRepository.getPresets() } returns listOf(mockk())

            val final = run().last()

            assertTrue(final.isFinished)
            assertFalse(final.isBlocked)
        }

    @Test
    fun `an unfinished sync is not reported as finished`() =
        runTest {
            coEvery { vocabularyRepository.seedFromAsset() } returns outcome(1)
            coEvery { presetRepository.seedFromAsset() } returns outcome(1)

            val emissions = run()

            assertFalse("the first emission cannot be finished", emissions.first().isFinished)
            assertTrue(emissions.last().isFinished)
        }
}
