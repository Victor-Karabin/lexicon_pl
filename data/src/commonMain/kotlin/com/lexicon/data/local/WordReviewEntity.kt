package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lexicon.model.scheduling.ReviewState

@Entity(tableName = "word_review", indices = [Index("dueAtEpochDay")])
data class WordReviewEntity(
    @PrimaryKey val wordId: Long,
    val repetitions: Int,
    val easeFactor: Double,
    val intervalDays: Long,
    val dueAtEpochDay: Long,
    val lapses: Int,
    val lastReviewedAtEpochMillis: Long,
)

fun WordReviewEntity.toState(): ReviewState =
    ReviewState(
        repetitions = repetitions,
        easeFactor = easeFactor,
        intervalDays = intervalDays,
        dueAtEpochDay = dueAtEpochDay,
        lapses = lapses,
    )

fun ReviewState.toEntity(
    wordId: Long,
    reviewedAtEpochMillis: Long,
): WordReviewEntity =
    WordReviewEntity(
        wordId = wordId,
        repetitions = repetitions,
        easeFactor = easeFactor,
        intervalDays = intervalDays,
        dueAtEpochDay = dueAtEpochDay,
        lapses = lapses,
        lastReviewedAtEpochMillis = reviewedAtEpochMillis,
    )
