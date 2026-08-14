package com.lexicon.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.lexicon.app.di.androidModule
import com.lexicon.app.di.networkModule
import com.lexicon.app.di.viewModelModule
import com.lexicon.data.di.dataAndroidModule
import com.lexicon.data.di.dataModule
import com.lexicon.domain.di.domainModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class LexiconApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@LexiconApplication)
            modules(androidModule, networkModule, dataModule, dataAndroidModule, domainModule, viewModelModule)
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
}
