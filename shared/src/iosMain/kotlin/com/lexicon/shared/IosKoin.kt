package com.lexicon.shared

import com.lexicon.application.di.domainModule
import com.lexicon.data.di.dataIosModule
import com.lexicon.data.di.dataModule
import org.koin.core.context.startKoin

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
