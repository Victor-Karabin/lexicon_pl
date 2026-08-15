package com.lexicon.boundary

enum class TrainingResultOutcomeBoundary {
    CORRECT,
    INCORRECT,
    SKIPPED,

    /**
     * The word was shown and nothing was asked.
     *
     * Not an answer, and deliberately not counted as one: it leaves the review
     * schedule alone and stays out of accuracy, because looking at a word is not
     * evidence of remembering it. It is still recorded, so the day knows the session
     * happened and the time spent counts as studying.
     */
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
