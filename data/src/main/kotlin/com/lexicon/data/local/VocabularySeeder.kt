package com.lexicon.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the words table from bundled assets on first use. Checking-then-inserting under a lock
 * (rather than a fire-and-forget seed on database creation) means callers that read words never
 * race an in-progress seed, and a table left empty by a schema wipe gets reseeded automatically.
 */
@Singleton
class VocabularySeeder
    @Inject
    constructor(
        private val wordDao: WordDao,
        private val vocabularySeedAssetLoader: VocabularySeedAssetLoader,
    ) {
        private val mutex = Mutex()

        suspend fun ensureSeeded() {
            mutex.withLock {
                if (wordDao.count() == 0) {
                    wordDao.insertAll(vocabularySeedAssetLoader.load())
                    return
                }
                // Rows a migration carried over predate the search key. Backfilling here rather
                // than in the migration keeps the folding in one place, and keeps the user's
                // favourites, which reseeding from scratch would discard.
                val stale = wordDao.getWithoutSearchKey()
                if (stale.isNotEmpty()) {
                    wordDao.updateAll(stale.map { it.copy(searchKey = searchKeyFor(it.text, it.translation)) })
                }
            }
        }
    }
