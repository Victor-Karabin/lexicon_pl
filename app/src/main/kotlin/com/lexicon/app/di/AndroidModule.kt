package com.lexicon.app.di

import com.lexicon.android.AndroidAudioPlayer
import com.lexicon.android.AndroidSpeechRecognizerService
import com.lexicon.android.AndroidSpeechSynthesizer
import com.lexicon.android.AudioPlayer
import com.lexicon.android.DefaultDispatcherProvider
import com.lexicon.android.SpeechRecognizerService
import com.lexicon.android.SpeechSynthesizer
import com.lexicon.android.SystemClock
import com.lexicon.common.Clock
import com.lexicon.common.DispatcherProvider
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

    @Binds
    @Singleton
    abstract fun bindAudioPlayer(impl: AndroidAudioPlayer): AudioPlayer
}
