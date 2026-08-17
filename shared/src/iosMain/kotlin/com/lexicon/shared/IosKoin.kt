package com.lexicon.shared

import com.lexicon.data.di.dataIosModule
import com.lexicon.data.di.dataModule
import com.lexicon.domain.di.domainModule
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
