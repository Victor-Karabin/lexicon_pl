package com.lexicon.interactors.training

/**
 * Whether a training has enough vocabulary to run.
 *
 * Checked before a session is built rather than after: a training given too few words does
 * not fail loudly, it quietly produces a shorter or easier session — an Image Test with two
 * options, or a session of one step — and the user has no way to tell that from the real thing.
 */
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
