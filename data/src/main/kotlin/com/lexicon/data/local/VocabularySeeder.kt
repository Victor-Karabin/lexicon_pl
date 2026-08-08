package com.lexicon.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the words table in line with the bundled asset.
 *
 * Seeding an empty table is only the first-run case. The corpus also *changes* between
 * releases — words added, corrected, or dropped — and an install that merely checked "is the
 * table empty?" would sit on whatever it was first seeded with for ever. That is exactly what
 * happened when the vocabulary grew from 1,767 to 2,219 words: existing installs saw none of
 * the new ones, and the levels those words filled in stayed empty.
 *
 * Reconciling rather than reseeding matters because the rows carry the user's favourites,
 * which are theirs and not the asset's to overwrite.
 */
@Singleton
class VocabularySeeder
    @Inject
    constructor(
        private val wordDao: WordDao,
        private val vocabularySeedAssetLoader: VocabularySeedAssetLoader,
        private val vocabularySyncStore: VocabularySyncStore,
    ) {
        private val mutex = Mutex()

        @Volatile
        private var syncedThisProcess = false

        suspend fun ensureSeeded() {
            if (syncedThisProcess) return
            mutex.withLock {
                if (syncedThisProcess) return

                val fingerprint = vocabularySeedAssetLoader.fingerprint()
                // The common launch: the asset has not moved since the last sync, so nothing is
                // parsed and nothing is read. The row count is still checked, because a schema
                // change can empty the table without the asset changing at all.
                if (fingerprint == vocabularySyncStore.syncedFingerprint() && wordDao.count() > 0) {
                    syncedThisProcess = true
                    return
                }

                reconcile()
                vocabularySyncStore.setSyncedFingerprint(fingerprint)
                syncedThisProcess = true
            }
        }

        private suspend fun reconcile() {
            val asset = vocabularySeedAssetLoader.load()
            val existing = wordDao.getAll().associateBy { it.id }

            if (existing.isEmpty()) {
                wordDao.insertAll(asset)
                return
            }

            val assetIds = asset.mapTo(mutableSetOf()) { it.id }
            wordDao.insertAll(asset.filter { it.id !in existing })

            // A word the asset no longer carries goes, and takes its heart with it — there is
            // no longer anything to study.
            val removed = existing.keys - assetIds
            if (removed.isNotEmpty()) wordDao.deleteByIds(removed.toList())

            // Everything else is refreshed from the asset except the favourite flag, which is
            // the user's and not the asset's to state. Only rows that actually differ are
            // written, so a corrected translation costs one update rather than two thousand.
            val changed = asset.mapNotNull { incoming ->
                val current = existing[incoming.id] ?: return@mapNotNull null
                incoming.copy(isFavourite = current.isFavourite).takeIf { it != current }
            }
            if (changed.isNotEmpty()) wordDao.updateAll(changed)
        }
    }
