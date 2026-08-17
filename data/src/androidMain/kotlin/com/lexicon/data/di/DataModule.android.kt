package com.lexicon.data.di

import com.lexicon.data.local.AppDatabaseBuilderFactory
import com.lexicon.data.local.AssetReader
import com.lexicon.data.local.DataStorePathResolver
import com.lexicon.data.remote.image.OpenverseImageSource
import com.lexicon.data.remote.image.PexelsImageSource
import com.lexicon.data.remote.image.PixabayImageSource
import com.lexicon.data.remote.image.RemoteImageSource
import com.lexicon.data.remote.image.UnsplashImageSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataAndroidModule = module {
    single { AppDatabaseBuilderFactory(androidContext()) }
    single { DataStorePathResolver(androidContext()) }
    single { AssetReader(androidContext()) }

    factory<List<RemoteImageSource>> {
        listOf(
            get<PexelsImageSource>(),
            get<PixabayImageSource>(),
            get<UnsplashImageSource>(),
            get<OpenverseImageSource>(),
        )
    }
}
