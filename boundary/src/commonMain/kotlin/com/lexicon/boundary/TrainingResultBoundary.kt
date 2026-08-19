package com.lexicon.boundary

import com.lexicon.model.training.StepOutcome

data class TrainingResultBoundary(
    val sessionId: String,
    val trainingType: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedAnswer: String,
    val submittedAnswer: String,
    val outcome: StepOutcome,
    val tipUsed: Boolean,
    val completedAtEpochMillis: Long,
    val wasReview: Boolean = false,
)
