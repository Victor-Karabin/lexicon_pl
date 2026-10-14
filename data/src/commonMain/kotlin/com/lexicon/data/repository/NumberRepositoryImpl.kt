package com.lexicon.data.repository

import com.lexicon.boundary.NumberProgressBoundary
import com.lexicon.boundary.NumberRepository
import com.lexicon.data.local.NumberCourseEntity
import com.lexicon.data.local.NumberDao
import com.lexicon.data.local.NumberProgressEntity

class NumberRepositoryImpl(
    private val dao: NumberDao,
) : NumberRepository {
    override suspend fun spec(): String? = dao.course()?.spec

    override suspend fun saveSpec(spec: String) = dao.saveCourse(NumberCourseEntity(spec = spec))

    override suspend fun progress(): List<NumberProgressBoundary> =
        dao.progress().map {
            NumberProgressBoundary(
                value = it.value,
                kind = it.kind,
                attempted = it.attempted,
                correct = it.correct,
                incorrect = it.incorrect,
                streak = it.streak,
            )
        }

    override suspend fun recordAttempt(
        value: Long,
        kind: String,
        isCorrect: Boolean,
    ) {
        val existing = dao.progressFor(value, kind)
        dao.save(
            NumberProgressEntity(
                value = value,
                kind = kind,
                attempted = (existing?.attempted ?: 0) + 1,
                correct = (existing?.correct ?: 0) + if (isCorrect) 1 else 0,
                incorrect = (existing?.incorrect ?: 0) + if (isCorrect) 0 else 1,
                streak = if (isCorrect) (existing?.streak ?: 0) + 1 else 0,
            ),
        )
    }
}
