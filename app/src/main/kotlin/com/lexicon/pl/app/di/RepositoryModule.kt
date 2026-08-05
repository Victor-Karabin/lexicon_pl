package com.lexicon.pl.app.di

import com.lexicon.pl.boundary.TrainingHistoryRepository
import com.lexicon.pl.boundary.VocabularyRepository
import com.lexicon.pl.data.repository.TrainingHistoryRepositoryImpl
import com.lexicon.pl.data.repository.VocabularyRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVocabularyRepository(impl: VocabularyRepositoryImpl): VocabularyRepository

    @Binds
    @Singleton
    abstract fun bindTrainingHistoryRepository(impl: TrainingHistoryRepositoryImpl): TrainingHistoryRepository
}
