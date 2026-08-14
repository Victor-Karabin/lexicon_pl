package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_results")
data class TrainingResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val trainingType: String,
    val stepIndex: Int,
    val vocabularyItemId: Long,
    val expectedAnswer: String,
    val submittedAnswer: String,
    val outcome: String,
    val tipUsed: Boolean,
    val completedAtEpochMillis: Long,
    /**
     * Whether the word already had a schedule when this was answered.
     *
     * Recorded rather than worked out later: retention is about words coming back,
     * and by the time anyone asks, the schedule has moved on and no longer says what
     * it said at the moment of the answer.
     */
    val wasReview: Boolean = false,
)
