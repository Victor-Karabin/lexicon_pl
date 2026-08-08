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

                // Words first, and presets only if there are words: a preset is a list of word
                // ids, so importing one over an empty vocabulary yields a catalogue of presets
                // that all resolve to nothing.
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

        /**
         * A step that throws is reported rather than propagated: the splash has to say what went
         * wrong, and a store that already holds rows can still be used with what it has —
         * a stale catalogue is not a broken app.
         */
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
