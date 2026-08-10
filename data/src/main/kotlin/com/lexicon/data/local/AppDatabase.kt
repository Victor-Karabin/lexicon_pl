package com.lexicon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WordEntity::class,
        TrainingResultEntity::class,
        ImageUrlCacheEntity::class,
        PresetCategoryEntity::class,
        PresetEntity::class,
        PresetWordEntity::class,
        DeletedPresetEntity::class,
        CourseEntity::class,
        LessonEntity::class,
        LessonSectionEntity::class,
        LessonWordEntity::class,
        LessonAudioEntity::class,
        LessonProgressEntity::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    abstract fun trainingResultDao(): TrainingResultDao

    abstract fun imageUrlCacheDao(): ImageUrlCacheDao

    abstract fun presetDao(): PresetDao

    abstract fun courseDao(): CourseDao
}
