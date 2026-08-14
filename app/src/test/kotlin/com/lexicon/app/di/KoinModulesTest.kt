package com.lexicon.app.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.lexicon.data.di.dataAndroidModule
import com.lexicon.data.di.dataModule
import com.lexicon.domain.di.domainModule
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Koin, unlike Hilt, does not verify its dependency graph at compile time — this is
 * the safety net that catches a missing/mistyped binding before it becomes a runtime
 * crash. Unlike the deprecated `checkModules()`, `verify()` checks constructor
 * parameter types statically without instantiating anything, so `Context` and
 * `SavedStateHandle` (normally supplied by `androidContext()` and the Android
 * ViewModel factory at real app runtime, not by these modules) are simply declared
 * as externally-provided types instead of needing fake instances.
 */
class KoinModulesTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `every Koin module's dependency graph resolves`() {
        module {
            includes(androidModule, networkModule, dataModule, dataAndroidModule, domainModule, viewModelModule)
        }.verify(extraTypes = listOf(Context::class, SavedStateHandle::class))
    }
}
