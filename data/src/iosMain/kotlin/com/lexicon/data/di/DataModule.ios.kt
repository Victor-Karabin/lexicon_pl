package com.lexicon.data.di

import com.lexicon.boundary.Translator
import com.lexicon.data.local.AppDatabaseBuilderFactory
import com.lexicon.data.local.AssetReader
import com.lexicon.data.local.DataStorePathResolver
import com.lexicon.data.remote.image.OpenverseIosImageSource
import com.lexicon.data.remote.image.PexelsIosImageSource
import com.lexicon.data.remote.image.PixabayIosImageSource
import com.lexicon.data.remote.image.RemoteImageSource
import com.lexicon.data.remote.translate.IosMyMemoryTranslator
import com.lexicon.data.repository.CorpusTranslatorImpl
import org.koin.dsl.module

fun dataIosModule(
    pexelsApiKey: String = "",
    pixabayApiKey: String = "",
) = module {
    single { AppDatabaseBuilderFactory() }
    single { DataStorePathResolver() }
    single { AssetReader() }

    factory<List<RemoteImageSource>> {
        listOf(
            PexelsIosImageSource(pexelsApiKey),
            PixabayIosImageSource(pixabayApiKey),
            OpenverseIosImageSource(),
        )
    }

    factory<List<Translator>>(translatorChainQualifier) {
        listOf(get<CorpusTranslatorImpl>(), IosMyMemoryTranslator())
    }
}
