package com.lexicon.interactors.crossword

data class StartCrosswordSessionRequest(
    val wordCount: Int = DEFAULT_WORD_COUNT,
) {
    companion object {
        const val DEFAULT_WORD_COUNT = 8
    }
}

interface StartCrosswordSessionUseCase {
    suspend operator fun invoke(request: StartCrosswordSessionRequest): CrosswordSessionResponse
}
