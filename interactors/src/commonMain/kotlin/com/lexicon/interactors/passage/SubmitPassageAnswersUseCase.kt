package com.lexicon.interactors.passage

data class SubmitPassageAnswersRequest(
    val sessionId: String,
    val passageId: String,
    val expected: List<String>,
    val answers: List<String>,
)

data class SubmitPassageAnswersResponse(
    val correct: List<Boolean>,
)

interface SubmitPassageAnswersUseCase {
    suspend operator fun invoke(request: SubmitPassageAnswersRequest): SubmitPassageAnswersResponse
}
