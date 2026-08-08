package com.lexicon.data.local

import com.lexicon.boundary.SyncOutcomeBoundary
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports the bundled preset catalogue into the database and keeps it current.
 *
 * The catalogue is replaced wholesale rather than reconciled row by row, unlike the vocabulary:
 * nothing the user owns lives on these rows — a preset's heart is stored on the words it
 * contains — so there is nothing to preserve, and a single transaction cannot leave a
 * half-written catalogue behind.
 */
@Singleton
class VocabularyPresetSeeder
    @Inject
    constructor(
        private val presetDao: PresetDao,
        private val loader: VocabularyPresetAssetLoader,
        private val syncStore: VocabularySyncStore,
    ) {
        private val mutex = Mutex()

        @Volatile
        private var syncedThisProcess = false

        suspend fun ensureSeeded() {
            if (syncedThisProcess) return
            sync()
        }

        /** Forgets a preset's deletion, so the next sync brings it back. */
        suspend fun restore(presetId: String) {
            presetDao.undeletePreset(presetId)
            syncStore.setSyncedPresetFingerprint("")
            syncedThisProcess = false
            ensureSeeded()
        }

        /** Records the deletion first, so a sync racing it cannot put the preset back. */
        suspend fun delete(presetId: String) {
            presetDao.insertDeletedPreset(DeletedPresetEntity(presetId))
            presetDao.deleteMemberships(presetId)
            presetDao.deletePreset(presetId)
        }

        suspend fun sync(): SyncOutcomeBoundary =
            mutex.withLock {
                val fingerprint = loader.fingerprint()
                val stored = presetDao.countPresets()
                if (fingerprint == syncStore.syncedPresetFingerprint() && stored > 0) {
                    syncedThisProcess = true
                    return@withLock SyncOutcomeBoundary(total = stored, added = 0, updated = 0, removed = 0)
                }

                val catalog = loader.load()
                // Deleted presets are dropped on the way in rather than removed afterwards, so
                // they never briefly exist — and so a re-import cannot resurrect them.
                val deleted = presetDao.getDeletedPresetIds().toSet()
                val presets = catalog.presets.filterNot { it.id in deleted }
                presetDao.replaceCatalog(
                    categories = catalog.categories.map { it.toEntity() },
                    presets = presets.map { it.toEntity() },
                    memberships = presets.flatMap { it.toMemberships() },
                )
                syncStore.setSyncedPresetFingerprint(fingerprint)
                syncedThisProcess = true

                // A replace has no meaningful update or delete count: every preset is written.
                // Reporting them as added is the honest description of what happened.
                SyncOutcomeBoundary(
                    total = presets.size,
                    added = presets.size - stored.coerceAtMost(presets.size),
                    updated = stored.coerceAtMost(presets.size),
                    removed = (stored - presets.size).coerceAtLeast(0),
                )
            }
    }
