package com.lexicon.boundary

import com.lexicon.model.scheduling.ReviewState

data class AccuracyBoundary(
    val answers: Int,
    val correct: Int,
) {
    val fraction: Double get() = if (answers == 0) 0.0 else correct.toDouble() / answers
}

data class StudyDayBoundary(
    val epochDay: Long,
    val studiedSeconds: Long,
    val newWords: Int,
    val reviews: Int,
    val answers: Int,
    val correctAnswers: Int,
)

interface ReviewScheduleRepository {
    suspend fun find(wordId: Long): ReviewState?

    suspend fun save(
        wordId: Long,
        state: ReviewState,
        reviewedAtEpochMillis: Long,
    )

    suspend fun dueWordIds(
        todayEpochDay: Long,
        limit: Int,
    ): List<Long>

    suspend fun scheduledWordIds(): Set<Long>

    suspend fun countLearned(): Int

    suspend fun countMastered(masteredIntervalDays: Long): Int
}

interface StudyRecordRepository {
    suspend fun record(
        epochDay: Long,
        addedSeconds: Long,
        wasNew: Boolean,
        wasCorrect: Boolean,
    )

    suspend fun day(epochDay: Long): StudyDayBoundary?

    suspend fun daysBetween(
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<StudyDayBoundary>

    suspend fun currentStreak(todayEpochDay: Long): Int
}
