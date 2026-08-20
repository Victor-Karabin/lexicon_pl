package com.lexicon.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

internal const val DATABASE_NAME = "lexicon.db"

expect class AppDatabaseBuilderFactory {
    fun create(): RoomDatabase.Builder<AppDatabase>
}

fun AppDatabaseBuilderFactory.buildAppDatabase(): AppDatabase =
    create()
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
