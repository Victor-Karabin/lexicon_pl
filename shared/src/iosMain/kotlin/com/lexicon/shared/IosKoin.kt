package com.lexicon.shared

import com.lexicon.data.di.dataIosModule
import com.lexicon.data.di.dataModule
import com.lexicon.domain.di.domainModule
import org.koin.core.context.startKoin

/**
 * Starts Koin for an iOS host. The equivalent of what LexiconApplication does on
 * Android, minus the Android-only modules (platform services, Compose ViewModels).
 *
 * The API keys are handed in rather than read here: the Kotlin side has no access to
 * the app bundle's build settings, and the host already reads them from
 * local.properties at build time. Blank is a valid answer — those sources then
 * answer with nothing and Openverse, which needs no key, carries the feature.
 */
fun initKoinIos(
    pexelsApiKey: String = "",
    pixabayApiKey: String = "",
) {
    startKoin {
        modules(
            dataModule,
            dataIosModule(pexelsApiKey = pexelsApiKey, pixabayApiKey = pixabayApiKey),
            domainModule,
        )
    }
}
