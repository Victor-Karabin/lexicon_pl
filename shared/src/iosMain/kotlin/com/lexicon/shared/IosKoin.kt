package com.lexicon.shared

import com.lexicon.data.di.dataIosModule
import com.lexicon.data.di.dataModule
import com.lexicon.domain.di.domainModule
import org.koin.core.context.startKoin

/**
 * Starts Koin for an iOS host. The equivalent of what LexiconApplication does on
 * Android, minus the Android-only modules (platform services, Compose ViewModels).
 */
fun initKoinIos() {
    startKoin {
        modules(dataModule, dataIosModule, domainModule)
    }
}
