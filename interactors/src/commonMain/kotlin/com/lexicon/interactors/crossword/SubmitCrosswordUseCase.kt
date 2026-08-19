package com.lexicon.interactors.crossword

import com.lexicon.interactors.training.StepOutcome

data class CrosswordWordSubmission(
    val vocabularyItemId: Long,
    val expectedText: String,
    val submittedText: String,
    val tipUsed: Boolean,
)

data class SubmitCrosswordRequest(
    val sessionId: String,
    val words: List<CrosswordWordSubmission>,
)

data class CrosswordWordResult(
    val vocabularyItemId: Long,
    val expectedText: String,
    val outcome: StepOutcome,
    val tipUsed: Boolean,
)

data class SubmitCrosswordResponse(
    val wordResults: List<CrosswordWordResult>,
    val isFullyCorrect: Boolean,
)

interface SubmitCrosswordUseCase {
    suspend operator fun invoke(request: SubmitCrosswordRequest): SubmitCrosswordResponse
}
