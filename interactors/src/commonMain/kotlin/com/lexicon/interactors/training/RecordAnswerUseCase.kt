package com.lexicon.interactors.training

import com.lexicon.model.training.StepOutcome

data class RecordedAnswer(
    val sessionId: String,
    val trainingType: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedAnswer: String,
    val submittedAnswer: String,
    val outcome: StepOutcome,
    val tipUsed: Boolean = false,
)

interface RecordAnswerUseCase {
    suspend operator fun invoke(answer: RecordedAnswer)
}
