package com.lexicon.interactors.mix

data class StartMixSessionRequest(
    val stepCount: Int? = null,
    val trainingTypes: Set<MixTrainingType> = MixTrainingType.entries.toSet(),
    // Empty means the whole study set; a lesson passes its own words here.
    val vocabularyIds: List<Long> = emptyList(),
)

interface StartMixSessionUseCase {
    suspend operator fun invoke(request: StartMixSessionRequest): MixSessionResponse
}
