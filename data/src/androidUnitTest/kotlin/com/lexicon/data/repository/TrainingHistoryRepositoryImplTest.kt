package com.lexicon.data.repository

import com.lexicon.boundary.TrainingResultBoundary
import com.lexicon.data.local.TrainingResultDao
import com.lexicon.data.local.TrainingResultEntity
import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingHistoryRepositoryImplTest {
    private val trainingResultDao: TrainingResultDao = mockk(relaxed = true)

    private val repository = TrainingHistoryRepositoryImpl(trainingResultDao)

    @Test
    fun `an answer is stored exactly as the domain reported it`() =
        runTest {
            repository.recordResult(
                TrainingResultBoundary(
                    sessionId = "s1",
                    trainingType = TrainingType.DICTATION,
                    stepIndex = 3,
                    vocabularyItemId = 7,
                    expectedAnswer = "woda",
                    submittedAnswer = "wode",
                    outcome = StepOutcome.INCORRECT,
                    tipUsed = true,
                    completedAtEpochMillis = NOW,
                    wasReview = true,
                ),
            )

            val stored = slot<TrainingResultEntity>()
            coVerify { trainingResultDao.insert(capture(stored)) }
            assertEquals("s1", stored.captured.sessionId)
            assertEquals("dictation", stored.captured.trainingType)
            assertEquals(3, stored.captured.stepIndex)
            assertEquals(7, stored.captured.vocabularyItemId)
            assertEquals("INCORRECT", stored.captured.outcome)
            assertTrue(stored.captured.tipUsed)
            assertTrue(stored.captured.wasReview)
            assertEquals(NOW, stored.captured.completedAtEpochMillis)
        }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
