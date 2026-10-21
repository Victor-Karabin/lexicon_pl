package com.lexicon.interactors.fillword

sealed interface FillwordSessionResult {
    data class Ready(
        val sessionId: String,
        val puzzle: FillwordPuzzle,
    ) : FillwordSessionResult

    data object EmptyStudySet : FillwordSessionResult
}

data class StartFillwordSessionRequest(
    val vocabularyIds: List<Long> = emptyList(),
)

interface StartFillwordSessionUseCase {
    suspend operator fun invoke(request: StartFillwordSessionRequest): FillwordSessionResult
}
