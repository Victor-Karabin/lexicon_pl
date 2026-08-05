package com.lexicon.pl.app.di

import com.lexicon.pl.android.AndroidSpeechRecognizerService
import com.lexicon.pl.android.AndroidSpeechSynthesizer
import com.lexicon.pl.android.DefaultDispatcherProvider
import com.lexicon.pl.android.SpeechRecognizerService
import com.lexicon.pl.android.SpeechSynthesizer
import com.lexicon.pl.android.SystemClock
import com.lexicon.pl.common.Clock
import com.lexicon.pl.common.DispatcherProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AndroidModule {
    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemClock): Clock

    @Binds
    @Singleton
    abstract fun bindSpeechSynthesizer(impl: AndroidSpeechSynthesizer): SpeechSynthesizer

    @Binds
    @Singleton
    abstract fun bindSpeechRecognizerService(impl: AndroidSpeechRecognizerService): SpeechRecognizerService
}
