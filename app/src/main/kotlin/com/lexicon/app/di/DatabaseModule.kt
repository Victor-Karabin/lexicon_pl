package com.lexicon.app.di

import android.content.Context
import androidx.room.Room
import com.lexicon.data.local.AppDatabase
import com.lexicon.data.local.ImageUrlCacheDao
import com.lexicon.data.local.PresetDao
import com.lexicon.data.local.TrainingResultDao
import com.lexicon.data.local.WordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "lexicon.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideWordDao(database: AppDatabase): WordDao = database.wordDao()

    @Provides
    fun provideTrainingResultDao(database: AppDatabase): TrainingResultDao = database.trainingResultDao()

    @Provides
    fun provideImageUrlCacheDao(database: AppDatabase): ImageUrlCacheDao = database.imageUrlCacheDao()

    @Provides
    fun providePresetDao(database: AppDatabase): PresetDao = database.presetDao()
}
