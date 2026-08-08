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

/**
 * Empties the words table so the seeder refills it from the current asset.
 *
 * The bundled vocabulary grew from a 20-word mock to the full corpus, and presets reference
 * words by id: an existing install that kept its old rows would show every preset resolving
 * to nothing. The seeder already reseeds an empty table, so clearing it is the whole fix.
 *
 * Training history is left alone. Its rows record which word id was answered, and those ids
 * now mean different words — the counts stay right, the per-word detail of old sessions does
 * not. Discarding a user's whole history to avoid that would be the worse trade.
 */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM `words`")
        }
    }

/** Adds the favourite flag. Existing rows default to not favourited, which keeps trainings
 * drawing from the whole vocabulary until the user marks something. */
val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `words` ADD COLUMN `isFavourite` INTEGER NOT NULL DEFAULT 0")
        }
    }
