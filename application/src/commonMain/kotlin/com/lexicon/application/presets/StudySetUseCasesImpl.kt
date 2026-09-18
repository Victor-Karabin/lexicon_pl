package com.lexicon.application.presets

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.ObserveWordStatusesUseCase
import com.lexicon.interactors.presets.SetWordStatusUseCase
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.WordStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SetWordStatusUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
) : SetWordStatusUseCase {
    override suspend fun invoke(
        id: VocabularyId,
        status: WordStatus,
    ) = vocabularyRepository.setStatus(listOf(id.value), status)
}

class ObserveWordStatusesUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
) : ObserveWordStatusesUseCase {
    override fun invoke(): Flow<Map<VocabularyId, WordStatus>> =
        vocabularyRepository.observeWordStatuses().map { statuses ->
            statuses.entries.associate { (id, status) -> VocabularyId(id) to status }
        }
}
