package com.lexicon.boundary

import kotlinx.coroutines.flow.Flow

data class VocabularyCourseBoundary(
    val newWordsADay: Int,
    val reviewsADay: Int,
    val queue: List<String>,
    val position: Int = 0,
    val round: Int = 0,
    val cardsSeenRound: Int = -1,
)

interface VocabularyCourseRepository {
    fun observe(): Flow<VocabularyCourseBoundary?>

    suspend fun get(): VocabularyCourseBoundary?

    suspend fun save(course: VocabularyCourseBoundary)
}
