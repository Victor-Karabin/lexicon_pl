package com.lexicon.domain.sync

import com.lexicon.boundary.SyncOutcomeBoundary
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.sync.CatalogSyncStatus
import com.lexicon.interactors.sync.SyncCatalogUseCase
import com.lexicon.interactors.sync.SyncStepStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

private const val SKIPPED = "Skipped because the vocabulary could not be loaded"

class SyncCatalogUseCaseImpl
    @Inject
    constructor(
        private val vocabularyRepository: VocabularyRepository,
        private val presetRepository: VocabularyPresetRepository,
    ) : SyncCatalogUseCase {
        override fun invoke(): Flow<CatalogSyncStatus> =
            flow {
                var status = CatalogSyncStatus(vocabulary = SyncStepStatus.InProgress)
                emit(status)

                val vocabulary = step(
                    sync = vocabularyRepository::syncFromSource,
                    storeHasData = { vocabularyRepository.countWords() > 0 },
                )
                status = status.copy(vocabulary = vocabulary)
                emit(status)

                if (vocabulary is SyncStepStatus.Failed && !vocabulary.canContinue) {
                    emit(status.copy(presets = SyncStepStatus.Failed(SKIPPED, canContinue = false)))
                    return@flow
                }

                status = status.copy(presets = SyncStepStatus.InProgress)
                emit(status)

                emit(
                    status.copy(
                        presets = step(
                            sync = presetRepository::syncFromSource,
                            storeHasData = { presetRepository.getPresets().isNotEmpty() },
                        ),
                    ),
                )
            }

        private suspend fun step(
            sync: suspend () -> SyncOutcomeBoundary,
            storeHasData: suspend () -> Boolean,
        ): SyncStepStatus =
            try {
                val outcome = sync()
                SyncStepStatus.Complete(
                    total = outcome.total,
                    added = outcome.added,
                    updated = outcome.updated,
                    removed = outcome.removed,
                )
            } catch (error: Exception) {
                SyncStepStatus.Failed(
                    reason = error.message ?: error::class.simpleName.orEmpty(),
                    canContinue = runCatching { storeHasData() }.getOrDefault(false),
                )
            }
    }
