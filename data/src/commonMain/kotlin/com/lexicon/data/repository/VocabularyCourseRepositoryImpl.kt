package com.lexicon.data.repository

import com.lexicon.boundary.VocabularyCourseBoundary
import com.lexicon.boundary.VocabularyCourseRepository
import com.lexicon.data.local.VocabularyCourseDao
import com.lexicon.data.local.toBoundary
import com.lexicon.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VocabularyCourseRepositoryImpl(
    private val dao: VocabularyCourseDao,
) : VocabularyCourseRepository {
    override fun observe(): Flow<VocabularyCourseBoundary?> = dao.observe().map { it?.toBoundary() }

    override suspend fun get(): VocabularyCourseBoundary? = dao.get()?.toBoundary()

    override suspend fun save(course: VocabularyCourseBoundary) = dao.save(course.toEntity())
}
