package com.lexicon.interactors.crossword

data class StartCrosswordSessionRequest(
    val wordCount: Int = DEFAULT_WORD_COUNT,
    // Empty means the whole study set; a lesson passes its own words here.
    val vocabularyIds: List<Long> = emptyList(),
) {
    companion object {
        const val DEFAULT_WORD_COUNT = 8
    }
}

interface StartCrosswordSessionUseCase {
    suspend operator fun invoke(request: StartCrosswordSessionRequest): CrosswordSessionResponse
}
