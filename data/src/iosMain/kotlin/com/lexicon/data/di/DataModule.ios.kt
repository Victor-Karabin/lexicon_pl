package com.lexicon.data.di

import com.lexicon.data.local.AppDatabaseBuilderFactory
import com.lexicon.data.local.AssetReader
import com.lexicon.data.local.DataStorePathResolver
import com.lexicon.data.remote.image.RemoteImageSource
import org.koin.dsl.module

/**
 * The iOS half of the data layer. File locations need no Context here, and there
 * are no image sources yet — the Retrofit-backed ones are Android-only, so image
 * lookups return null until a Ktor implementation exists.
 */
val dataIosModule = module {
    single { AppDatabaseBuilderFactory() }
    single { DataStorePathResolver() }
    single { AssetReader() }

    factory<List<RemoteImageSource>> { emptyList() }
}
