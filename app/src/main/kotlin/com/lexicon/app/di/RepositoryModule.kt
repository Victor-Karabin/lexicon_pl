package com.lexicon.app.di

import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.data.repository.CachingImageProviderImpl
import com.lexicon.data.repository.TrainingHistoryRepositoryImpl
import com.lexicon.data.repository.VocabularyRepositoryImpl
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

    @Binds
    @Singleton
    abstract fun bindImageProvider(impl: CachingImageProviderImpl): ImageProvider
}
