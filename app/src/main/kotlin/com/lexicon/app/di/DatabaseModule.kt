package com.lexicon.app.di

import android.content.Context
import androidx.room.Room
import com.lexicon.data.local.AppDatabase
import com.lexicon.data.local.ImageUrlCacheDao
import com.lexicon.data.local.SeedingDatabaseCallback
import com.lexicon.data.local.TrainingResultDao
import com.lexicon.data.local.VocabularySeedAssetLoader
import com.lexicon.data.local.WordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Provider
import javax.inject.Singleton

private const val DATABASE_NAME = "lexicon.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideApplicationScope(dispatchers: com.lexicon.common.DispatcherProvider): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatchers.io)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<AppDatabase>,
        applicationScope: CoroutineScope,
        vocabularySeedAssetLoader: VocabularySeedAssetLoader,
    ): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addCallback(SeedingDatabaseCallback(databaseProvider, applicationScope, vocabularySeedAssetLoader))
            // Pre-release app; the words table is always reseeded from bundled JSON assets, so a
            // schema bump can just drop and recreate rather than carrying a migration.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideWordDao(database: AppDatabase): WordDao = database.wordDao()

    @Provides
    fun provideTrainingResultDao(database: AppDatabase): TrainingResultDao = database.trainingResultDao()

    @Provides
    fun provideImageUrlCacheDao(database: AppDatabase): ImageUrlCacheDao = database.imageUrlCacheDao()
}
