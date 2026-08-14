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
 * The schema is still moving, so there are no migrations: a version bump drops
 * every table and the seeders refill what they can (see CourseSeeder,
 * VocabularySeeder, VocabularyPresetSeeder, ProgramSeeder).
 *
 * What the seeders cannot refill goes with it — favourites, deleted words,
 * membership overrides, hand-written words and presets, training history, review
 * schedules, study days and program state all live only here. That is the accepted
 * trade while the schema is in motion; it is worth revisiting before anyone is
 * relying on the app to remember anything.
 */
fun AppDatabaseBuilderFactory.buildAppDatabase(): AppDatabase =
    create()
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
