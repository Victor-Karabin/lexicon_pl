package com.lexicon.interactors.course

import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonProgress
import com.lexicon.model.course.LessonScreen
import com.lexicon.model.course.LessonScript
import com.lexicon.model.course.WritingReview

fun interface GetLessonScriptUseCase {
    suspend operator fun invoke(id: LessonId): LessonScript?
}

fun interface GetLessonProgressUseCase {
    suspend operator fun invoke(id: LessonId): LessonProgress?
}

fun interface SaveLessonProgressUseCase {
    suspend operator fun invoke(
        id: LessonId,
        progress: LessonProgress,
    )
}

sealed interface WritingReviewOutcome {
    data class Reviewed(val review: WritingReview) : WritingReviewOutcome

    data object Offline : WritingReviewOutcome

    data object Unavailable : WritingReviewOutcome
}

fun interface ReviewWritingUseCase {
    suspend operator fun invoke(
        screen: LessonScreen.FreeWriting,
        answers: List<String>,
    ): WritingReviewOutcome
}
