package com.lexicon.app.di

import com.lexicon.android.AndroidAudioPlayer
import com.lexicon.android.AndroidSpeechRecognizerService
import com.lexicon.android.AndroidSpeechSynthesizer
import com.lexicon.android.AudioPlayer
import com.lexicon.android.DefaultDispatcherProvider
import com.lexicon.android.LessonAudioLibrary
import com.lexicon.android.LessonAudioPlayer
import com.lexicon.android.SpeechRecognizerService
import com.lexicon.android.SpeechSynthesizer
import com.lexicon.android.SystemClock
import com.lexicon.common.Clock
import com.lexicon.common.DispatcherProvider
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val androidModule = module {
    singleOf(::DefaultDispatcherProvider) { bind<DispatcherProvider>() }
    singleOf(::SystemClock) { bind<Clock>() }
    singleOf(::AndroidSpeechSynthesizer) { bind<SpeechSynthesizer>() }
    singleOf(::AndroidSpeechRecognizerService) { bind<SpeechRecognizerService>() }
    singleOf(::AndroidAudioPlayer) { bind<AudioPlayer>() }

    // Not yet behind an interface (see the KMP migration plan's Phase 4) — bound by
    // concrete type in the meantime.
    singleOf(::LessonAudioLibrary)
    singleOf(::LessonAudioPlayer)
}
