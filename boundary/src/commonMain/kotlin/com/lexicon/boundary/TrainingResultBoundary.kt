package com.lexicon.boundary

enum class TrainingResultOutcomeBoundary {
    CORRECT,
    INCORRECT,
    SKIPPED,

    SEEN,
}

data class TrainingResultBoundary(
    val sessionId: String,
    val trainingType: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedAnswer: String,
    val submittedAnswer: String,
    val outcome: TrainingResultOutcomeBoundary,
    val tipUsed: Boolean,
    val completedAtEpochMillis: Long,
)
