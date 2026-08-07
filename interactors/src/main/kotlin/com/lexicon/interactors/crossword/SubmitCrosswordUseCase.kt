package com.lexicon.interactors.crossword

enum class CrosswordWordOutcome { CORRECT, INCORRECT }

data class CrosswordWordSubmission(
    val vocabularyItemId: Long,
    val expectedText: String,
    /** Letters currently filled into this word's cells, including any Tip-revealed ones. */
    val submittedText: String,
    /** Whether at least one of this word's letters was revealed via Tip. */
    val tipUsed: Boolean,
)

data class SubmitCrosswordRequest(
    val sessionId: String,
    val words: List<CrosswordWordSubmission>,
)

data class CrosswordWordResult(
    val vocabularyItemId: Long,
    val expectedText: String,
    val outcome: CrosswordWordOutcome,
    val tipUsed: Boolean,
)

data class SubmitCrosswordResponse(
    val wordResults: List<CrosswordWordResult>,
    /** Correct only when every word is correct and no Tip was used anywhere in the puzzle. */
    val isFullyCorrect: Boolean,
)

interface SubmitCrosswordUseCase {
    suspend operator fun invoke(request: SubmitCrosswordRequest): SubmitCrosswordResponse
}
