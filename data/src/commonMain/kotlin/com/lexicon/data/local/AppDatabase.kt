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
        LessonEntity::class,
        LessonWordEntity::class,
        LessonAudioEntity::class,
        LessonProgressEntity::class,
        LessonExerciseEntity::class,
        LessonExerciseItemEntity::class,
        WordReviewEntity::class,
        StudyDayEntity::class,
        ProgramEntity::class,
        ProgramEnrolmentEntity::class,
        ProgramDayEntity::class,
        ProgramMilestoneEntity::class,
        ProgramRewardEntity::class,
        ConjugationSelectionEntity::class,
        ConjugationProgressEntity::class,
        ConjugationImageEntity::class,
    ],
    version = 21,
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

    abstract fun programDao(): ProgramDao

    abstract fun conjugationDao(): ConjugationDao
}

@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
