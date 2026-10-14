package com.lexicon.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        WordEntity::class,
        TrainingResultEntity::class,
        ImageUrlCacheEntity::class,
        PresetCategoryEntity::class,
        PresetEntity::class,
        PresetWordEntity::class,
        DeletedPresetEntity::class,
        PresetWordOverrideEntity::class,
        CourseEntity::class,
        DashboardCourseEntity::class,
        LessonEntity::class,
        LessonWordEntity::class,
        LessonAudioEntity::class,
        LessonProgressEntity::class,
        LessonScriptProgressEntity::class,
        LessonExerciseEntity::class,
        LessonExerciseItemEntity::class,
        WordReviewEntity::class,
        StudyDayEntity::class,
        VocabularyCourseEntity::class,
        ConjugationVerbEntity::class,
        ConjugationCourseEntity::class,
        ConjugationCourseVerbEntity::class,
        ConjugationProgressEntity::class,
        NumberCourseEntity::class,
        NumberProgressEntity::class,
    ],
    version = 41,
    exportSchema = false,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    abstract fun trainingResultDao(): TrainingResultDao

    abstract fun imageUrlCacheDao(): ImageUrlCacheDao

    abstract fun presetDao(): PresetDao

    abstract fun courseDao(): CourseDao

    abstract fun wordReviewDao(): WordReviewDao

    abstract fun studyDayDao(): StudyDayDao

    abstract fun vocabularyCourseDao(): VocabularyCourseDao

    abstract fun conjugationDao(): ConjugationDao

    abstract fun numberDao(): NumberDao
}

@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
