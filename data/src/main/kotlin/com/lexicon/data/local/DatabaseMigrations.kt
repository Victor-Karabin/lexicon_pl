package com.lexicon.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `image_url_cache` " +
                    "(`query` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, PRIMARY KEY(`query`))",
            )
        }
    }
