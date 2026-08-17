package com.lexicon.interactors.passage

import kotlinx.collections.immutable.ImmutableList

data class StartPassageSessionRequest(
    val passageId: String? = null,
    val withWordBank: Boolean = false,
)

data class PassageSessionResponse(
    val sessionId: String,
    val passage: Passage,
    val bank: ImmutableList<String>,
)

interface StartPassageSessionUseCase {
    suspend operator fun invoke(request: StartPassageSessionRequest): PassageSessionResponse?
}
