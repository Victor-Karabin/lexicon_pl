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

/** Adds the favourite flag. Existing rows default to not favourited, so a user has to choose
 * a study set before training — see [com.lexicon.boundary.VocabularyRepository.getRandomItems]. */
val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `words` ADD COLUMN `isFavourite` INTEGER NOT NULL DEFAULT 0")
        }
    }

/**
 * Adds the search key. Unlike [MIGRATION_2_3] this keeps the rows: they now carry the user's
 * favourites, so wiping the table to force a reseed would throw away their study set. The
 * seeder fills the column in when it next reconciles the table against the asset.
 */
val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `words` ADD COLUMN `searchKey` TEXT NOT NULL DEFAULT ''")
        }
    }

/**
 * Adds the CEFR band. Like [MIGRATION_4_5] this keeps the rows, because they carry the user's
 * favourites, and lets the seeder fill the column in from the asset when it next reconciles.
 */
val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `words` ADD COLUMN `cefr` TEXT NOT NULL DEFAULT ''")
        }
    }
