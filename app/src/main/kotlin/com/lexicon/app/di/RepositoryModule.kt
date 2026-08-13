package com.lexicon.app.di

import com.lexicon.boundary.CourseRepository
import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.data.local.CourseAssetLoader
import com.lexicon.data.local.CourseSeeder
import com.lexicon.data.local.VocabularyPresetAssetLoader
import com.lexicon.data.local.VocabularyPresetSeeder
import com.lexicon.data.local.VocabularySeedAssetLoader
import com.lexicon.data.local.VocabularySeeder
import com.lexicon.data.local.VocabularySyncStore
import com.lexicon.data.repository.CachingImageProviderImpl
import com.lexicon.data.repository.CourseRepositoryImpl
import com.lexicon.data.repository.FallbackImageProviderImpl
import com.lexicon.data.repository.TrainingHistoryRepositoryImpl
import com.lexicon.data.repository.VocabularyPresetRepositoryImpl
import com.lexicon.data.repository.VocabularyRepositoryImpl
import com.lexicon.data.settings.SettingsRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::VocabularyRepositoryImpl) { bind<VocabularyRepository>() }
    singleOf(::TrainingHistoryRepositoryImpl) { bind<TrainingHistoryRepository>() }
    singleOf(::CachingImageProviderImpl) { bind<ImageProvider>() }
    singleOf(::SettingsRepositoryImpl) { bind<SettingsRepository>() }
    factoryOf(::VocabularyPresetRepositoryImpl) { bind<VocabularyPresetRepository>() }
    singleOf(::CourseRepositoryImpl) { bind<CourseRepository>() }

    factoryOf(::FallbackImageProviderImpl)

    factoryOf(::VocabularySeedAssetLoader)
    factoryOf(::VocabularyPresetAssetLoader)
    factoryOf(::CourseAssetLoader)

    singleOf(::VocabularySyncStore)
    singleOf(::VocabularySeeder)
    singleOf(::VocabularyPresetSeeder)
    singleOf(::CourseSeeder)
}
