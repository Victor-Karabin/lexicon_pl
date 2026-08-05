package com.lexicon.pl.app.di

import android.content.Context
import androidx.room.Room
import com.lexicon.pl.data.local.AppDatabase
import com.lexicon.pl.data.local.SeedingDatabaseCallback
import com.lexicon.pl.data.local.TrainingResultDao
import com.lexicon.pl.data.local.WordDao
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
    fun provideApplicationScope(dispatchers: com.lexicon.pl.common.DispatcherProvider): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatchers.io)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<AppDatabase>,
        applicationScope: CoroutineScope,
    ): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addCallback(SeedingDatabaseCallback(databaseProvider, applicationScope))
            .build()

    @Provides
    fun provideWordDao(database: AppDatabase): WordDao = database.wordDao()

    @Provides
    fun provideTrainingResultDao(database: AppDatabase): TrainingResultDao = database.trainingResultDao()
}
