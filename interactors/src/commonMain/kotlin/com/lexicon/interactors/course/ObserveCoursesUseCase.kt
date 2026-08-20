package com.lexicon.interactors.course

import com.lexicon.model.course.Course
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

interface ObserveCoursesUseCase {
    operator fun invoke(): Flow<ImmutableList<Course>>
}
