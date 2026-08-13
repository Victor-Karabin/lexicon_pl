package com.lexicon.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

internal const val DATABASE_NAME = "lexicon.db"

/**
 * Opens the database file, which is the one part of Room that cannot be shared:
 * Android resolves the name against a Context, iOS against a documents-directory
 * path. Everything past the builder — driver, migration policy — is common.
 */
expect class AppDatabaseBuilderFactory {
    fun create(): RoomDatabase.Builder<AppDatabase>
}

/**
 * The schema is still moving, so there are no migrations: the database falls back
 * to destructive migration and the seeders refill it (see CourseSeeder,
 * VocabularySeeder, VocabularyPresetSeeder).
 */
fun AppDatabaseBuilderFactory.buildAppDatabase(): AppDatabase =
    create()
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
