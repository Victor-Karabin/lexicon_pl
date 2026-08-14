package com.lexicon.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * Keeps the shipped program catalogue in step with the asset.
 *
 * The same shape as [VocabularyPresetSeeder]: fingerprint the asset, rewrite the
 * catalogue when it changes, do nothing when it has not. What the learner has done
 * inside a program — enrolled, generated days, reached milestones, earned rewards —
 * lives in its own tables keyed by program id and is never touched here, so a
 * re-seed cannot lose any of it.
 */
class ProgramSeeder(
    private val programDao: ProgramDao,
    private val loader: ProgramAssetLoader,
    private val syncStore: VocabularySyncStore,
) {
    private val mutex = Mutex()

    @Volatile
    private var syncedThisProcess = false

    suspend fun ensureSeeded() {
        if (syncedThisProcess) return
        sync()
    }

    suspend fun sync() {
        mutex.withLock {
            val fingerprint = loader.fingerprint()
            if (fingerprint == syncStore.syncedProgramFingerprint() && programDao.countPrograms() > 0) {
                syncedThisProcess = true
                return@withLock
            }

            programDao.replaceCatalog(loader.load().programs.map { it.toEntity() })
            syncStore.setSyncedProgramFingerprint(fingerprint)
            syncedThisProcess = true
        }
    }
}
