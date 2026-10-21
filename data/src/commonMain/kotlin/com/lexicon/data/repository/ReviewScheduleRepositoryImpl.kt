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

    override suspend fun knownWordSchedules(): Map<Long, ReviewState> =
        wordReviewDao.knownSchedules().associate { it.wordId to it.toState() }
}
