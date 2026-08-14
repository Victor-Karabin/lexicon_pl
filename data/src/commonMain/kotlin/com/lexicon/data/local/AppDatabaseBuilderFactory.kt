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
 * Adds review scheduling and the daily study record, and marks past results as not
 * having been reviews.
 *
 * Written out for the same reason as the migration above, and more so: word_review
 * and study_day hold the learner's memory of every word and every day they have
 * studied, and no asset can rebuild either. Existing results default to wasReview = 0,
 * which reads as "we do not know", and is right — nothing before this knew.
 */
private val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE training_results ADD COLUMN wasReview INTEGER NOT NULL DEFAULT 0")
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS word_review (
                wordId INTEGER NOT NULL PRIMARY KEY,
                repetitions INTEGER NOT NULL,
                easeFactor REAL NOT NULL,
                intervalDays INTEGER NOT NULL,
                dueAtEpochDay INTEGER NOT NULL,
                lapses INTEGER NOT NULL,
                lastReviewedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_word_review_dueAtEpochDay ON word_review (dueAtEpochDay)")
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS study_day (
                epochDay INTEGER NOT NULL PRIMARY KEY,
                studiedSeconds INTEGER NOT NULL,
                newWords INTEGER NOT NULL,
                reviews INTEGER NOT NULL,
                answers INTEGER NOT NULL,
                correctAnswers INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}

/**
 * Adds the program catalogue and the learner's state within it.
 *
 * Only the catalogue is refilled from the asset; enrolments, generated days,
 * milestones and rewards are the learner's record of working through a program and
 * exist nowhere else.
 */
private val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS programs (
                id TEXT NOT NULL PRIMARY KEY,
                level TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                titleJson TEXT NOT NULL,
                descriptionJson TEXT NOT NULL,
                difficulty TEXT NOT NULL,
                estimatedDays INTEGER NOT NULL,
                visibility TEXT NOT NULL,
                configJson TEXT NOT NULL,
                isUserCreated INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_programs_level ON programs (level)")
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS program_enrolment (
                programId TEXT NOT NULL PRIMARY KEY,
                startedAtEpochDay INTEGER NOT NULL,
                status TEXT NOT NULL,
                completedAtEpochDay INTEGER
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS program_day (
                programId TEXT NOT NULL,
                epochDay INTEGER NOT NULL,
                activitiesJson TEXT NOT NULL,
                appliedRulesJson TEXT NOT NULL,
                isComplete INTEGER NOT NULL,
                PRIMARY KEY (programId, epochDay)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_program_day_programId ON program_day (programId)")
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS program_milestone (
                programId TEXT NOT NULL,
                milestoneId TEXT NOT NULL,
                achievedAtEpochDay INTEGER NOT NULL,
                PRIMARY KEY (programId, milestoneId)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_program_milestone_programId ON program_milestone (programId)")
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS program_reward (
                programId TEXT NOT NULL,
                rewardId TEXT NOT NULL,
                grantedAtEpochDay INTEGER NOT NULL,
                PRIMARY KEY (programId, rewardId)
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_program_reward_programId ON program_reward (programId)")
    }
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
        .addMigrations(MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
