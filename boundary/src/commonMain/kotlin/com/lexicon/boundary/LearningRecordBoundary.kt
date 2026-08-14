package com.lexicon.boundary

/** Answers and how many were right, over some window. */
data class AccuracyBoundary(
    val answers: Int,
    val correct: Int,
) {
    val fraction: Double get() = if (answers == 0) 0.0 else correct.toDouble() / answers
}

/** One word's place in the review schedule. */
data class WordScheduleBoundary(
    val wordId: Long,
    val repetitions: Int,
    val easeFactor: Double,
    val intervalDays: Long,
    val dueAtEpochDay: Long,
    val lapses: Int,
)

/** What one day of studying amounted to. */
data class StudyDayBoundary(
    val epochDay: Long,
    val studiedSeconds: Long,
    val newWords: Int,
    val reviews: Int,
    val answers: Int,
    val correctAnswers: Int,
)

/**
 * When words are due, and how many are known.
 *
 * Reads only. The schedule is written as a side effect of answering, by
 * [TrainingHistoryRepository], so that every training feeds it without knowing it
 * exists.
 */
interface ReviewScheduleRepository {
    suspend fun dueWordIds(
        todayEpochDay: Long,
        limit: Int,
    ): List<Long>

    suspend fun schedule(wordId: Long): WordScheduleBoundary?

    /** Words answered at least once. */
    suspend fun countLearned(): Int

    /** Words whose interval has outgrown the mastery threshold. */
    suspend fun countMastered(masteredIntervalDays: Long): Int
}

/** Days studied, and what they contained. */
interface StudyRecordRepository {
    suspend fun day(epochDay: Long): StudyDayBoundary?

    suspend fun daysBetween(
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<StudyDayBoundary>

    /**
     * Days studied in an unbroken run ending today, or yesterday — a streak should
     * not be reported as broken until the day it is actually missed.
     */
    suspend fun currentStreak(todayEpochDay: Long): Int
}
