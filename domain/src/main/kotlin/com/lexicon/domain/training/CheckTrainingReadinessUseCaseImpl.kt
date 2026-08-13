package com.lexicon.domain.training

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.training.CheckTrainingReadinessUseCase
import com.lexicon.interactors.training.TrainingReadiness

class CheckTrainingReadinessUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
) : CheckTrainingReadinessUseCase {
    override suspend fun invoke(
        minimumWords: Int,
        excludePhrases: Boolean,
    ): TrainingReadiness {
        val available = vocabularyRepository.countStudyWords(excludePhrases)
        return if (available >= minimumWords) {
            TrainingReadiness.Ready
        } else {
            TrainingReadiness.NotEnoughWords(required = minimumWords, available = available)
        }
    }
}
