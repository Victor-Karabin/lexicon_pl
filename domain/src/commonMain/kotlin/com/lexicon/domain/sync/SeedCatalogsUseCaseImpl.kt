package com.lexicon.domain.sync

import com.lexicon.boundary.CatalogSeedGate
import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.CourseRepository
import com.lexicon.boundary.SeedOutcomeBoundary
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.sync.CatalogSeedStatus
import com.lexicon.interactors.sync.SeedCatalogsUseCase
import com.lexicon.interactors.sync.SeedStepStatus
import com.lexicon.interactors.sync.isBlocked
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val SKIPPED = "Skipped because the vocabulary could not be loaded"

class SeedCatalogsUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val presetRepository: VocabularyPresetRepository,
    private val courseRepository: CourseRepository,
    private val conjugationRepository: ConjugationRepository,
    private val gate: CatalogSeedGate,
) : SeedCatalogsUseCase {
    override fun invoke(): Flow<CatalogSeedStatus> =
        flow {
            if (gate.isCurrent()) {
                emit(alreadyCurrent())
                return@flow
            }

            var status = CatalogSeedStatus(vocabulary = SeedStepStatus.InProgress)
            emit(status)

            val vocabulary = step(
                sync = vocabularyRepository::seedFromAsset,
                storeHasData = { vocabularyRepository.countWords() > 0 },
            )
            status = status.copy(vocabulary = vocabulary)
            emit(status)

            if (vocabulary is SeedStepStatus.Failed && !vocabulary.canContinue) {
                emit(
                    status.copy(
                        presets = SeedStepStatus.Failed(SKIPPED, canContinue = false),
                        course = SeedStepStatus.Failed(SKIPPED, canContinue = false),
                        verbs = SeedStepStatus.Failed(SKIPPED, canContinue = false),
                    ),
                )
                return@flow
            }

            status = status.copy(presets = SeedStepStatus.InProgress)
            emit(status)

            status = status.copy(
                presets = step(
                    sync = presetRepository::seedFromAsset,
                    storeHasData = { presetRepository.getPresets().isNotEmpty() },
                ),
            )
            emit(status)

            status = status.copy(course = SeedStepStatus.InProgress)
            emit(status)

            status = status.copy(
                course = step(
                    sync = courseRepository::seedFromAsset,
                    storeHasData = { courseRepository.countLessons() > 0 },
                ),
            )
            emit(status)

            status = status.copy(verbs = SeedStepStatus.InProgress)
            emit(status)

            status = status.copy(
                verbs = step(
                    sync = conjugationRepository::seedFromAsset,
                    storeHasData = { conjugationRepository.countVerbs() > 0 },
                ),
            )

            if (!status.isBlocked) gate.markCurrent()
            emit(status)
        }

    private suspend fun alreadyCurrent(): CatalogSeedStatus =
        CatalogSeedStatus(
            vocabulary = settled { vocabularyRepository.countWords() },
            presets = settled { presetRepository.getPresets().size },
            course = settled { courseRepository.countLessons() },
            verbs = settled { conjugationRepository.countVerbs() },
        )

    private suspend fun settled(count: suspend () -> Int): SeedStepStatus =
        SeedStepStatus.Complete(
            total = runCatching { count() }.getOrDefault(0),
            added = 0,
            updated = 0,
            removed = 0,
        )

    private suspend fun step(
        sync: suspend () -> SeedOutcomeBoundary,
        storeHasData: suspend () -> Boolean,
    ): SeedStepStatus =
        try {
            val outcome = sync()
            SeedStepStatus.Complete(
                total = outcome.total,
                added = outcome.added,
                updated = outcome.updated,
                removed = outcome.removed,
            )
        } catch (error: Exception) {
            SeedStepStatus.Failed(
                reason = error.message ?: error::class.simpleName.orEmpty(),
                canContinue = runCatching { storeHasData() }.getOrDefault(false),
            )
        }
}
