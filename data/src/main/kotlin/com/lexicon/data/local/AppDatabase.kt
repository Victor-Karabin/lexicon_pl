package com.lexicon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WordEntity::class, TrainingResultEntity::class, ImageUrlCacheEntity::class],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    abstract fun trainingResultDao(): TrainingResultDao

    abstract fun imageUrlCacheDao(): ImageUrlCacheDao
}
