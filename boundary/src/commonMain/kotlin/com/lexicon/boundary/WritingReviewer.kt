package com.lexicon.boundary

import com.lexicon.model.course.WritingReview

data class WrittenAnswerBoundary(
    val label: String,
    val text: String,
)

data class WritingReviewRequestBoundary(
    val task: String,
    val answers: List<WrittenAnswerBoundary>,
    val model: List<String>,
    val criteria: List<String>,
)

sealed interface WritingReviewResultBoundary {
    data class Reviewed(val review: WritingReview) : WritingReviewResultBoundary

    data object Offline : WritingReviewResultBoundary

    data class Refused(val reason: String) : WritingReviewResultBoundary
}

interface WritingReviewer {
    suspend fun review(request: WritingReviewRequestBoundary): WritingReviewResultBoundary
}
