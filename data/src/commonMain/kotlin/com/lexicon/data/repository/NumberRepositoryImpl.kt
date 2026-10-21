package com.lexicon.data.repository

import com.lexicon.boundary.NumberProgressBoundary
import com.lexicon.boundary.NumberRepository
import com.lexicon.data.local.AttemptCounts
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
                gender = it.gender,
                attempted = it.attempted,
                correct = it.correct,
                incorrect = it.incorrect,
                streak = it.streak,
            )
        }

    override suspend fun recordAttempt(
        value: Long,
        kind: String,
        gender: String,
        isCorrect: Boolean,
    ) {
        val existing = dao.progressFor(value, kind, gender)
        val counts = AttemptCounts.of(existing).after(isCorrect)
        dao.save(
            NumberProgressEntity(
                value = value,
                kind = kind,
                gender = gender,
                attempted = counts.attempted,
                correct = counts.correct,
                incorrect = counts.incorrect,
                streak = counts.streak,
            ),
        )
    }
}
