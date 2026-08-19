package com.lexicon.domain.sync

import com.lexicon.boundary.CatalogSyncGate
import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.CourseRepository
import com.lexicon.boundary.SyncOutcomeBoundary
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.sync.CatalogSyncStatus
import com.lexicon.interactors.sync.SyncCatalogUseCase
import com.lexicon.interactors.sync.SyncStepStatus
import com.lexicon.interactors.sync.isBlocked
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val SKIPPED = "Skipped because the vocabulary could not be loaded"

class SyncCatalogUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val presetRepository: VocabularyPresetRepository,
    private val courseRepository: CourseRepository,
    private val conjugationRepository: ConjugationRepository,
    private val gate: CatalogSyncGate,
) : SyncCatalogUseCase {
    override fun invoke(): Flow<CatalogSyncStatus> =
        flow {
            if (gate.isCurrent()) {
                emit(alreadyCurrent())
                return@flow
            }

            var status = CatalogSyncStatus(vocabulary = SyncStepStatus.InProgress)
            emit(status)

            val vocabulary = step(
                sync = vocabularyRepository::syncFromSource,
                storeHasData = { vocabularyRepository.countWords() > 0 },
            )
            status = status.copy(vocabulary = vocabulary)
            emit(status)

            if (vocabulary is SyncStepStatus.Failed && !vocabulary.canContinue) {
                emit(
                    status.copy(
                        presets = SyncStepStatus.Failed(SKIPPED, canContinue = false),
                        course = SyncStepStatus.Failed(SKIPPED, canContinue = false),
                        verbs = SyncStepStatus.Failed(SKIPPED, canContinue = false),
                    ),
                )
                return@flow
            }

            status = status.copy(presets = SyncStepStatus.InProgress)
            emit(status)

            status = status.copy(
                presets = step(
                    sync = presetRepository::syncFromSource,
                    storeHasData = { presetRepository.getPresets().isNotEmpty() },
                ),
            )
            emit(status)

            status = status.copy(course = SyncStepStatus.InProgress)
            emit(status)

            status = status.copy(
                course = step(
                    sync = courseRepository::syncFromSource,
                    storeHasData = { courseRepository.countLessons() > 0 },
                ),
            )
            emit(status)

            status = status.copy(verbs = SyncStepStatus.InProgress)
            emit(status)

            status = status.copy(
                verbs = step(
                    sync = conjugationRepository::syncFromSource,
                    storeHasData = { conjugationRepository.countVerbs() > 0 },
                ),
            )

            if (!status.isBlocked) gate.markCurrent()
            emit(status)
        }

    private suspend fun alreadyCurrent(): CatalogSyncStatus =
        CatalogSyncStatus(
            vocabulary = settled { vocabularyRepository.countWords() },
            presets = settled { presetRepository.getPresets().size },
            course = settled { courseRepository.countLessons() },
            verbs = settled { conjugationRepository.countVerbs() },
        )

    private suspend fun settled(count: suspend () -> Int): SyncStepStatus =
        SyncStepStatus.Complete(
            total = runCatching { count() }.getOrDefault(0),
            added = 0,
            updated = 0,
            removed = 0,
        )

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
