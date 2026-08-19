package com.lexicon.interactors.training

import com.lexicon.model.training.StepOutcome
import com.lexicon.model.training.TrainingType

data class RecordedAnswer(
    val sessionId: String,
    val trainingType: TrainingType,
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
