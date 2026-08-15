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

/**
 * The iOS half of the data layer. File locations need no Context here.
 *
 * [pexelsApiKey] and [pixabayApiKey] come from the host app, which reads them from
 * local.properties the same way the Android build does; blank keys make those two
 * sources answer with nothing, leaving Openverse, which needs no key at all.
 */
fun dataIosModule(
    pexelsApiKey: String = "",
    pixabayApiKey: String = "",
) = module {
    single { AppDatabaseBuilderFactory() }
    single { DataStorePathResolver() }
    single { AssetReader() }

    // Ordered the way Android orders them: the keyed sources give better pictures,
    // and Openverse is the one that always answers.
    factory<List<RemoteImageSource>> {
        listOf(
            PexelsIosImageSource(pexelsApiKey),
            PixabayIosImageSource(pixabayApiKey),
            OpenverseIosImageSource(),
        )
    }

    // The corpus first, then the memory: an offline hit is instant and always right,
    // and only a word the corpus has never seen is worth a network call.
    factory<List<Translator>>(translatorChainQualifier) {
        listOf(get<CorpusTranslatorImpl>(), IosMyMemoryTranslator())
    }
}
