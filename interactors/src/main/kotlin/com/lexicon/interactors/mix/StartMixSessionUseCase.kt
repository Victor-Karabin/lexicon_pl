package com.lexicon.interactors.mix

data class StartMixSessionRequest(
    val stepCount: Int? = null,
    val trainingTypes: Set<MixTrainingType> = MixTrainingType.entries.toSet(),
)

interface StartMixSessionUseCase {
    suspend operator fun invoke(request: StartMixSessionRequest): MixSessionResponse
}
