package com.lexicon.interactors.mix

data class StartMixSessionRequest(
    /** Null uses the step count configured in Settings; pass a value only to override it. */
    val stepCount: Int? = null,
    /** Restricts which training types may be generated; defaults to every supported type. */
    val trainingTypes: Set<MixTrainingType> = MixTrainingType.entries.toSet(),
)

interface StartMixSessionUseCase {
    suspend operator fun invoke(request: StartMixSessionRequest): MixSessionResponse
}
