package com.lexicon.pl.domain.pronunciation

import com.lexicon.pl.boundary.TrainingHistoryRepository
import com.lexicon.pl.common.Clock
import com.lexicon.pl.domain.dictation.AnswerNormalizer
import com.lexicon.pl.interactors.pronunciation.PronunciationStepOutcome
import com.lexicon.pl.interactors.pronunciation.SubmitPronunciationResultRequest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitPronunciationResultUseCaseImplTest {
    private val trainingHistoryRepository: TrainingHistoryRepository = mockk(relaxed = true)
    private val clock: Clock = mockk { every { nowEpochMillis() } returns 1_000L }
    private val useCase = SubmitPronunciationResultUseCaseImpl(trainingHistoryRepository, AnswerNormalizer(), clock)

    private fun request(
        recognizedText: String = "kot",
        confidence: Float? = null,
        tipUsed: Boolean = false,
        skipped: Boolean = false,
    ) = SubmitPronunciationResultRequest(
        sessionId = "session-1",
        stepIndex = 0,
        vocabularyItemId = 1L,
        expectedText = "kot",
        recognizedText = recognizedText,
        confidence = confidence,
        tipUsed = tipUsed,
        skipped = skipped,
    )

    @Test
    fun `confidence at or above the 70pct threshold is Correct`() =
        runTest {
            val response = useCase(request(confidence = 0.7f))
            assertEquals(PronunciationStepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `confidence below the threshold is Incorrect even if text matches`() =
        runTest {
            val response = useCase(request(recognizedText = "kot", confidence = 0.5f))
            assertEquals(PronunciationStepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `no confidence falls back to text matching`() =
        runTest {
            val response = useCase(request(recognizedText = "Kot", confidence = null))
            assertEquals(PronunciationStepOutcome.CORRECT, response.outcome)
        }

    @Test
    fun `tip forces Incorrect even with a high confidence`() =
        runTest {
            val response = useCase(request(confidence = 0.99f, tipUsed = true))
            assertEquals(PronunciationStepOutcome.INCORRECT, response.outcome)
        }

    @Test
    fun `skip is Skipped regardless of confidence`() =
        runTest {
            val response = useCase(request(confidence = 0.99f, skipped = true))
            assertEquals(PronunciationStepOutcome.SKIPPED, response.outcome)
        }
}
