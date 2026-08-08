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

        suspend fun sync(): SyncOutcomeBoundary =
            mutex.withLock {
                val fingerprint = loader.fingerprint()
                val stored = presetDao.countPresets()
                if (fingerprint == syncStore.syncedPresetFingerprint() && stored > 0) {
                    syncedThisProcess = true
                    return@withLock SyncOutcomeBoundary(total = stored, added = 0, updated = 0, removed = 0)
                }

                val catalog = loader.load()
                presetDao.replaceCatalog(
                    categories = catalog.categories.map { it.toEntity() },
                    presets = catalog.presets.map { it.toEntity() },
                    memberships = catalog.presets.flatMap { it.toMemberships() },
                )
                syncStore.setSyncedPresetFingerprint(fingerprint)
                syncedThisProcess = true

                // A replace has no meaningful update or delete count: every preset is written.
                // Reporting them as added is the honest description of what happened.
                SyncOutcomeBoundary(
                    total = catalog.presets.size,
                    added = catalog.presets.size - stored.coerceAtMost(catalog.presets.size),
                    updated = stored.coerceAtMost(catalog.presets.size),
                    removed = (stored - catalog.presets.size).coerceAtLeast(0),
                )
            }
    }
