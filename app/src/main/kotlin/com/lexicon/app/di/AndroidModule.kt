package com.lexicon.app.di

import android.content.Context
import com.lexicon.BuildConfig
import com.lexicon.android.audio.AndroidAudioPlayer
import com.lexicon.android.audio.AndroidAudioRecorder
import com.lexicon.android.audio.AudioRecorder
import com.lexicon.android.cloud.CloudSpeechApi
import com.lexicon.android.lesson.AndroidLessonAudioLibrary
import com.lexicon.android.lesson.AndroidLessonAudioPlayer
import com.lexicon.android.recognition.AndroidSpeechRecognizerService
import com.lexicon.android.recognition.CloudSpeechRecognizerService
import com.lexicon.android.speech.AndroidSpeechStore
import com.lexicon.android.speech.AndroidSpeechSynthesizer
import com.lexicon.android.speech.CloudSpeechSynthesizer
import com.lexicon.android.speech.SpeechStore
import com.lexicon.android.speech.VoicePreference
import com.lexicon.boundary.AppVersionProvider
import com.lexicon.boundary.AudioPlayer
import com.lexicon.boundary.LessonAudioLibrary
import com.lexicon.boundary.LessonAudioPlayer
import com.lexicon.boundary.SettingsRepository
import com.lexicon.boundary.SpeechRecognizerService
import com.lexicon.boundary.SpeechSynthesizer
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val androidModule = module {
    single<AppVersionProvider> { AppVersionProvider { BuildConfig.VERSION_CODE } }

    single<VoicePreference> { VoicePreference { get<SettingsRepository>().getSettings().voiceId } }

    single<SpeechStore> { AndroidSpeechStore(context = get()) }

    single { CloudSpeechApi(apiKey = BuildConfig.GOOGLE_TTS_API_KEY) }

    single<SpeechSynthesizer> {
        CloudSpeechSynthesizer(
            api = get(),
            store = get(),
            player = get(),
            settings = get(),
            fallback = AndroidSpeechSynthesizer(context = get(), settings = get()),
            dispatchers = get(),
        )
    }
    single<AudioRecorder> { AndroidAudioRecorder(cacheDirectory = get<Context>().cacheDir, dispatchers = get()) }

    single<SpeechRecognizerService> {
        CloudSpeechRecognizerService(
            recorder = get(),
            api = get(),
            device = AndroidSpeechRecognizerService(context = get(), dispatchers = get()),
            dispatchers = get(),
        )
    }
    singleOf(::AndroidAudioPlayer) { bind<AudioPlayer>() }

    singleOf(::AndroidLessonAudioLibrary) { bind<LessonAudioLibrary>() }
    singleOf(::AndroidLessonAudioPlayer) { bind<LessonAudioPlayer>() }
}
