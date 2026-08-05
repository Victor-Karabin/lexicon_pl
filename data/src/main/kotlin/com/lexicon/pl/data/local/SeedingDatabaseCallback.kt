package com.lexicon.pl.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * Seeds the bundled vocabulary JSON asset into a freshly-created database.
 *
 * Takes a [Provider] rather than the database directly to avoid a circular dependency:
 * this callback is registered while the database is still being built, and `onCreate`
 * only fires once the instance the provider resolves to already exists.
 */
class SeedingDatabaseCallback
    @Inject
    constructor(
        private val databaseProvider: Provider<AppDatabase>,
        private val applicationScope: CoroutineScope,
        private val vocabularySeedAssetLoader: VocabularySeedAssetLoader,
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            applicationScope.launch {
                databaseProvider.get().wordDao().insertAll(vocabularySeedAssetLoader.load())
            }
        }
    }
