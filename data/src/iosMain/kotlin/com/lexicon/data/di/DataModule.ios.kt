package com.lexicon.data.di

import com.lexicon.boundary.Translator
import com.lexicon.data.local.AppDatabaseBuilderFactory
import com.lexicon.data.local.AssetReader
import com.lexicon.data.local.DataStorePathResolver
import com.lexicon.data.remote.image.RemoteImageSource
import com.lexicon.data.repository.CorpusTranslatorImpl
import org.koin.dsl.module

/**
 * The iOS half of the data layer. File locations need no Context here, and there
 * are no image sources yet — the Retrofit-backed ones are Android-only, so image
 * lookups return null until a Ktor implementation exists. Translation is offline
 * only for the same reason: the corpus lookup works, the remote one is Android's.
 */
val dataIosModule = module {
    single { AppDatabaseBuilderFactory() }
    single { DataStorePathResolver() }
    single { AssetReader() }

    factory<List<RemoteImageSource>> { emptyList() }
    factory<List<Translator>>(translatorChainQualifier) { listOf(get<CorpusTranslatorImpl>()) }
}
