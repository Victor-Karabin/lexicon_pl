package com.lexicon.interactors.training

sealed interface TrainingReadiness {
    data object Ready : TrainingReadiness

    data class NotEnoughWords(
        val required: Int,
        val available: Int,
    ) : TrainingReadiness
}

interface CheckTrainingReadinessUseCase {
    suspend operator fun invoke(minimumWords: Int): TrainingReadiness
}
