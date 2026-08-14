package com.lexicon.data.di

import com.lexicon.boundary.CourseRepository
import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.common.DefaultDispatcherProvider
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.SystemClock
import com.lexicon.data.local.AppDatabase
import com.lexicon.data.local.AppDatabaseBuilderFactory
import com.lexicon.data.local.CourseAssetLoader
import com.lexicon.data.local.CourseSeeder
import com.lexicon.data.local.DataStorePathResolver
import com.lexicon.data.local.SETTINGS_STORE_NAME
import com.lexicon.data.local.VOCABULARY_SYNC_STORE_NAME
import com.lexicon.data.local.VocabularyPresetAssetLoader
import com.lexicon.data.local.VocabularyPresetSeeder
import com.lexicon.data.local.VocabularySeedAssetLoader
import com.lexicon.data.local.VocabularySeeder
import com.lexicon.data.local.VocabularySyncStore
import com.lexicon.data.local.buildAppDatabase
import com.lexicon.data.local.createDataStore
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
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val settingsDataStoreQualifier = named(SETTINGS_STORE_NAME)
internal val vocabularySyncDataStoreQualifier = named(VOCABULARY_SYNC_STORE_NAME)

/**
 * Everything in the data layer that is the same on every platform. What is not —
 * how the database and preferences files are located, and which image APIs exist —
 * comes from the platform's own module (dataAndroidModule / dataIosModule), which
 * has to be loaded alongside this one.
 */
val dataModule = module {
    // Both have multiplatform implementations in :common, so they are bound here
    // rather than per-platform.
    singleOf(::DefaultDispatcherProvider) { bind<DispatcherProvider>() }
    singleOf(::SystemClock) { bind<Clock>() }

    single { get<AppDatabaseBuilderFactory>().buildAppDatabase() }
    factory { get<AppDatabase>().wordDao() }
    factory { get<AppDatabase>().trainingResultDao() }
    factory { get<AppDatabase>().imageUrlCacheDao() }
    factory { get<AppDatabase>().presetDao() }
    factory { get<AppDatabase>().courseDao() }

    single(settingsDataStoreQualifier) { get<DataStorePathResolver>().createDataStore(SETTINGS_STORE_NAME) }
    single(vocabularySyncDataStoreQualifier) { get<DataStorePathResolver>().createDataStore(VOCABULARY_SYNC_STORE_NAME) }

    singleOf(::VocabularyRepositoryImpl) { bind<VocabularyRepository>() }
    singleOf(::TrainingHistoryRepositoryImpl) { bind<TrainingHistoryRepository>() }
    singleOf(::CachingImageProviderImpl) { bind<ImageProvider>() }
    single<SettingsRepository> { SettingsRepositoryImpl(get(settingsDataStoreQualifier)) }
    factoryOf(::VocabularyPresetRepositoryImpl) { bind<VocabularyPresetRepository>() }
    singleOf(::CourseRepositoryImpl) { bind<CourseRepository>() }

    // The source list itself is platform-supplied; the fallback logic is not.
    singleOf(::FallbackImageProviderImpl)

    factoryOf(::VocabularySeedAssetLoader)
    factoryOf(::VocabularyPresetAssetLoader)
    factoryOf(::CourseAssetLoader)

    single { VocabularySyncStore(get(vocabularySyncDataStoreQualifier)) }
    singleOf(::VocabularySeeder)
    singleOf(::VocabularyPresetSeeder)
    singleOf(::CourseSeeder)
}
