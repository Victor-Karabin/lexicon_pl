package com.lexicon.data.local

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

internal const val DATABASE_NAME = "lexicon.db"

/**
 * Adds the two flags that mark a word or preset as the learner's own.
 *
 * Written out rather than left to the destructive fallback below because by 16 the
 * database holds things no asset can put back: which words are favourites, which
 * were deleted, the membership edits in preset_word_overrides, training history and
 * lesson progress. Dropping all of that to add two columns that default to 0 would
 * be a poor trade.
 */
private val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE words ADD COLUMN isUserCreated INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE presets ADD COLUMN isUserCreated INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Opens the database file, which is the one part of Room that cannot be shared:
 * Android resolves the name against a Context, iOS against a documents-directory
 * path. Everything past the builder — driver, migration policy — is common.
 */
expect class AppDatabaseBuilderFactory {
    fun create(): RoomDatabase.Builder<AppDatabase>
}

/**
 * Anything older than 16 still falls back to a destructive migration and is refilled
 * by the seeders (see CourseSeeder, VocabularySeeder, VocabularyPresetSeeder) — the
 * schema moved freely up to that point and writing migrations back through it would
 * be work for nobody's benefit.
 *
 * From 16 on that fallback is a last resort rather than the plan. Words and presets
 * the learner writes exist only here: no asset can re-seed them, so a destructive
 * step loses them outright. New schema versions want a [Migration] like the one above.
 */
fun AppDatabaseBuilderFactory.buildAppDatabase(): AppDatabase =
    create()
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_16_17)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
