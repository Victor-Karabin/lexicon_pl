package com.lexicon.data.remote.sentence

import com.lexicon.boundary.WritingReviewRequestBoundary
import com.lexicon.boundary.WritingReviewResultBoundary
import com.lexicon.boundary.WritingReviewer

class OpenAiWritingReviewer(
    private val api: OpenAiApi,
) : WritingReviewer {
    override suspend fun review(request: WritingReviewRequestBoundary): WritingReviewResultBoundary =
        api.ask(writingReviewPrompt(request)).toReviewResult()
}
