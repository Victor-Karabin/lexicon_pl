package com.lexicon.interactors.passage

data class SubmitPassageAnswersRequest(
    val sessionId: String,
    val expected: List<String>,
    val answers: List<String>,
    val words: List<String>,
)

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
