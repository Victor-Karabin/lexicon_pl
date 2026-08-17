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
    val wasReview: Boolean = false,
)
