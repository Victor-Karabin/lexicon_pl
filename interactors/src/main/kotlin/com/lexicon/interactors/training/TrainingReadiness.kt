package com.lexicon.interactors.training

sealed interface TrainingReadiness {
    data object Ready : TrainingReadiness

    data class NotEnoughWords(
        val required: Int,
        val available: Int,
    ) : TrainingReadiness
}

interface CheckTrainingReadinessUseCase {
    /** [excludePhrases] narrows the check to single words — what Crossword can place. */
    suspend operator fun invoke(
        minimumWords: Int,
        excludePhrases: Boolean = false,
    ): TrainingReadiness
}
