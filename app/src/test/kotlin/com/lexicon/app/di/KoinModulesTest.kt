package com.lexicon.app.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.lexicon.application.di.domainModule
import com.lexicon.data.di.dataAndroidModule
import com.lexicon.data.di.dataModule
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

class KoinModulesTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `every Koin module's dependency graph resolves`() {
        module {
            includes(androidModule, networkModule, dataModule, dataAndroidModule, domainModule, viewModelModule)
        }.verify(extraTypes = listOf(Context::class, SavedStateHandle::class))
    }
}
