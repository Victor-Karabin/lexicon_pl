package com.lexicon.domain.training

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.training.TrainingReadiness
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckTrainingReadinessUseCaseImplTest {
    private val vocabularyRepository: VocabularyRepository = mockk()
    private val useCase = CheckTrainingReadinessUseCaseImpl(vocabularyRepository)

    private fun withStudySetOf(count: Int) {
        coEvery { vocabularyRepository.countStudyWords() } returns count
    }

    @Test
    fun `a study set larger than the requirement is ready`() =
        runTest {
            withStudySetOf(50)

            assertEquals(TrainingReadiness.Ready, useCase(minimumWords = 6))
        }

    @Test
    fun `a study set exactly the size of the requirement is ready`() =
        runTest {
            withStudySetOf(6)

            assertEquals(TrainingReadiness.Ready, useCase(minimumWords = 6))
        }

    @Test
    fun `one word short is not ready, and reports both numbers`() =
        runTest {
            withStudySetOf(5)

            assertEquals(TrainingReadiness.NotEnoughWords(required = 6, available = 5), useCase(minimumWords = 6))
        }

    @Test
    fun `an empty study set is not ready`() =
        runTest {
            withStudySetOf(0)

            assertEquals(TrainingReadiness.NotEnoughWords(required = 1, available = 0), useCase(minimumWords = 1))
        }

    @Test
    fun `readiness is measured against the study set, not the whole vocabulary`() =
        runTest {
            withStudySetOf(3)

            val readiness = useCase(minimumWords = 8)

            assertEquals(TrainingReadiness.NotEnoughWords(required = 8, available = 3), readiness)
        }
}
