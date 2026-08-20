package com.lexicon.data.repository

import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.data.local.WordReviewDao
import com.lexicon.data.local.toEntity
import com.lexicon.data.local.toState
import com.lexicon.model.scheduling.ReviewState

class ReviewScheduleRepositoryImpl(
    private val wordReviewDao: WordReviewDao,
) : ReviewScheduleRepository {
    override suspend fun find(wordId: Long): ReviewState? = wordReviewDao.find(wordId)?.toState()

    override suspend fun save(
        wordId: Long,
        state: ReviewState,
        reviewedAtEpochMillis: Long,
    ) {
        wordReviewDao.upsert(state.toEntity(wordId, reviewedAtEpochMillis))
    }

    override suspend fun dueWordIds(
        todayEpochDay: Long,
        limit: Int,
    ): List<Long> = wordReviewDao.dueWordIds(todayEpochDay, limit)

    override suspend fun scheduledWordIds(): Set<Long> = wordReviewDao.allScheduledWordIds().toSet()

    override suspend fun countLearned(): Int = wordReviewDao.countLearned()

    override suspend fun countMastered(masteredIntervalDays: Long): Int = wordReviewDao.countMastered(masteredIntervalDays)
}
