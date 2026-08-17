package com.lexicon.app.di

import com.lexicon.android.AndroidAudioPlayer
import com.lexicon.android.AndroidLessonAudioLibrary
import com.lexicon.android.AndroidLessonAudioPlayer
import com.lexicon.android.AndroidSpeechRecognizerService
import com.lexicon.android.AndroidSpeechSynthesizer
import com.lexicon.android.AudioPlayer
import com.lexicon.android.LessonAudioLibrary
import com.lexicon.android.LessonAudioPlayer
import com.lexicon.android.SpeechRecognizerService
import com.lexicon.android.SpeechSynthesizer
import com.lexicon.android.VoicePreference
import com.lexicon.boundary.SettingsRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val androidModule = module {
    single<SpeechSynthesizer> {
        AndroidSpeechSynthesizer(
            context = get(),
            settings = VoicePreference { get<SettingsRepository>().getSettings().voiceId },
        )
    }
    singleOf(::AndroidSpeechRecognizerService) { bind<SpeechRecognizerService>() }
    singleOf(::AndroidAudioPlayer) { bind<AudioPlayer>() }

    singleOf(::AndroidLessonAudioLibrary) { bind<LessonAudioLibrary>() }
    singleOf(::AndroidLessonAudioPlayer) { bind<LessonAudioPlayer>() }
}
