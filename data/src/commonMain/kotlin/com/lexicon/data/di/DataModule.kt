package com.lexicon.data.di

import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.CourseRepository
import com.lexicon.boundary.ImageProvider
import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.StudyRecordRepository
import com.lexicon.boundary.TrainingHistoryRepository
import com.lexicon.boundary.Translator
import com.lexicon.boundary.VocabularyPresetRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.Clock
import com.lexicon.common.DefaultDispatcherProvider
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.SystemClock
import com.lexicon.data.local.AppDatabase
import com.lexicon.data.local.AppDatabaseBuilderFactory
import com.lexicon.data.local.ConjugationAssetLoader
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
import com.lexicon.data.repository.ConjugationRepositoryImpl
import com.lexicon.data.repository.CorpusTranslatorImpl
import com.lexicon.data.repository.CourseRepositoryImpl
import com.lexicon.data.repository.FallbackImageProviderImpl
import com.lexicon.data.repository.FallbackTranslatorImpl
import com.lexicon.data.repository.ProgramRepositoryImpl
import com.lexicon.data.repository.ReviewScheduleRepositoryImpl
import com.lexicon.data.repository.StudyRecordRepositoryImpl
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

val translatorChainQualifier = named("translator-chain")

val dataModule = module {

    singleOf(::DefaultDispatcherProvider) { bind<DispatcherProvider>() }
    singleOf(::SystemClock) { bind<Clock>() }

    single { get<AppDatabaseBuilderFactory>().buildAppDatabase() }
    factory { get<AppDatabase>().wordDao() }
    factory { get<AppDatabase>().trainingResultDao() }
    factory { get<AppDatabase>().imageUrlCacheDao() }
    factory { get<AppDatabase>().presetDao() }
    factory { get<AppDatabase>().courseDao() }
    factory { get<AppDatabase>().wordReviewDao() }
    factory { get<AppDatabase>().studyDayDao() }
    factory { get<AppDatabase>().programDao() }
    factory { get<AppDatabase>().conjugationDao() }

    single(settingsDataStoreQualifier) { get<DataStorePathResolver>().createDataStore(SETTINGS_STORE_NAME) }
    single(vocabularySyncDataStoreQualifier) { get<DataStorePathResolver>().createDataStore(VOCABULARY_SYNC_STORE_NAME) }

    singleOf(::VocabularyRepositoryImpl) { bind<VocabularyRepository>() }
    singleOf(::TrainingHistoryRepositoryImpl) { bind<TrainingHistoryRepository>() }
    singleOf(::ReviewScheduleRepositoryImpl) { bind<ReviewScheduleRepository>() }
    singleOf(::StudyRecordRepositoryImpl) { bind<StudyRecordRepository>() }
    factoryOf(::ProgramRepositoryImpl) { bind<ProgramRepository>() }
    singleOf(::CachingImageProviderImpl) { bind<ImageProvider>() }
    single<SettingsRepository> { SettingsRepositoryImpl(get(settingsDataStoreQualifier)) }
    factoryOf(::VocabularyPresetRepositoryImpl) { bind<VocabularyPresetRepository>() }
    singleOf(::CourseRepositoryImpl) { bind<CourseRepository>() }

    singleOf(::FallbackImageProviderImpl)

    singleOf(::CorpusTranslatorImpl)
    single<Translator> { FallbackTranslatorImpl(get(translatorChainQualifier)) }

    factoryOf(::VocabularySeedAssetLoader)
    factoryOf(::VocabularyPresetAssetLoader)
    factoryOf(::ConjugationAssetLoader)
    singleOf(::ConjugationRepositoryImpl) { bind<ConjugationRepository>() }
    factoryOf(::CourseAssetLoader)

    single { VocabularySyncStore(get(vocabularySyncDataStoreQualifier)) }
    singleOf(::VocabularySeeder)
    singleOf(::VocabularyPresetSeeder)
    singleOf(::CourseSeeder)
}
