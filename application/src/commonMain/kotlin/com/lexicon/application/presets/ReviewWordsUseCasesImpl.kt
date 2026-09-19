package com.lexicon.application.presets

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.CountWordsToReviewUseCase
import com.lexicon.interactors.presets.GetWordsToReviewUseCase
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class GetWordsToReviewUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
) : GetWordsToReviewUseCase {
    override suspend fun invoke(limit: Int): ImmutableList<Word> =
        vocabularyRepository.wordsWithStatus(WordStatus.UNDEFINED, limit).toImmutableList()
}

class CountWordsToReviewUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
) : CountWordsToReviewUseCase {
    override suspend fun invoke(): Int = vocabularyRepository.countWithStatus(WordStatus.UNDEFINED)
}
