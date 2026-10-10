package com.lexicon.interactors.course

import com.lexicon.model.course.CourseId

fun interface SetCourseOnDashboardUseCase {
    suspend operator fun invoke(
        id: CourseId,
        isShown: Boolean,
    )
}
