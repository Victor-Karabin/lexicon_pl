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

/**
 * The Android half of the data layer: file locations come from a Context, and the
 * four Retrofit-backed image APIs are wired in the order they should be tried.
 * The Retrofit/OkHttp clients themselves are still built in the app module, which
 * is where the BuildConfig API keys live.
 */
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
