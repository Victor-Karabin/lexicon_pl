package com.lexicon.interactors.passage

data class SubmitPassageAnswersRequest(
    val sessionId: String,
    val expected: List<String>,
    val answers: List<String>,
    /** The favourite behind each gap, since the gap itself holds an inflected form. */
    val words: List<String>,
)

/** One gap, marked, with enough about the word to list it on the result screen. */
data class PassageGapResult(
    val expected: String,
    val submitted: String,
    val translation: String,
    val isCorrect: Boolean,
)

data class SubmitPassageAnswersResponse(
    val results: List<PassageGapResult>,
) {
    val correct: List<Boolean> get() = results.map { it.isCorrect }
}

interface SubmitPassageAnswersUseCase {
    suspend operator fun invoke(request: SubmitPassageAnswersRequest): SubmitPassageAnswersResponse
}
