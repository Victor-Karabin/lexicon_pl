package com.lexicon.interactors.passage

import kotlinx.collections.immutable.ImmutableList

data class StartPassageSessionRequest(
    val withWordBank: Boolean = false,
    val stepCount: Int? = null,
)

sealed interface PassageSessionResult {
    data class Ready(
        val sessionId: String,
        val passage: Passage,
        val bank: ImmutableList<String>,
    ) : PassageSessionResult

    data object EmptyStudySet : PassageSessionResult

    data object Offline : PassageSessionResult

    data class Refused(val reason: String) : PassageSessionResult
}

interface StartPassageSessionUseCase {
    suspend operator fun invoke(request: StartPassageSessionRequest): PassageSessionResult
}
