package com.lexicon.app.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.lexicon.BuildConfig
import com.lexicon.boundary.FillwordGenerator
import com.lexicon.boundary.SentenceGenerator
import com.lexicon.boundary.Translator
import com.lexicon.data.di.translatorChainQualifier
import com.lexicon.data.remote.image.OpenverseApi
import com.lexicon.data.remote.image.OpenverseImageSource
import com.lexicon.data.remote.image.PexelsApi
import com.lexicon.data.remote.image.PexelsImageSource
import com.lexicon.data.remote.image.PixabayApi
import com.lexicon.data.remote.image.PixabayImageSource
import com.lexicon.data.remote.image.UnsplashApi
import com.lexicon.data.remote.image.UnsplashImageSource
import com.lexicon.data.remote.sentence.OpenAiApi
import com.lexicon.data.remote.sentence.OpenAiFillwordGenerator
import com.lexicon.data.remote.sentence.OpenAiSentenceGenerator
import com.lexicon.data.remote.translate.DeepLApi
import com.lexicon.data.remote.translate.DeepLTranslator
import com.lexicon.data.remote.translate.MyMemoryApi
import com.lexicon.data.remote.translate.MyMemoryTranslator
import com.lexicon.data.repository.CorpusTranslatorImpl
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import retrofit2.Retrofit

private const val PEXELS_BASE_URL = "https://api.pexels.com/"
private const val PIXABAY_BASE_URL = "https://pixabay.com/"
private const val UNSPLASH_BASE_URL = "https://api.unsplash.com/"
private const val OPENVERSE_BASE_URL = "https://api.openverse.org/"
private const val DEEPL_BASE_URL = "https://api-free.deepl.com/"
private const val OPENAI_BASE_URL = "https://api.openai.com/"
private const val MYMEMORY_BASE_URL = "https://api.mymemory.translated.net/"

private fun headerInterceptor(
    name: String,
    value: String,
) = Interceptor { chain ->
    chain.proceed(chain.request().newBuilder().addHeader(name, value).build())
}

private fun queryParamInterceptor(
    name: String,
    value: String,
) = Interceptor { chain ->
    val original = chain.request()
    val url = original.url.newBuilder().addQueryParameter(name, value).build()
    chain.proceed(original.newBuilder().url(url).build())
}

private fun retrofit(
    baseUrl: String,
    client: OkHttpClient,
    json: Json,
): Retrofit =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

val networkModule = module {
    single { Json { ignoreUnknownKeys = true } }

    single {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    single {
        val client =
            get<OkHttpClient>().newBuilder()
                .addInterceptor(headerInterceptor("Authorization", BuildConfig.PEXELS_API_KEY))
                .build()
        retrofit(PEXELS_BASE_URL, client, get()).create(PexelsApi::class.java)
    }

    single {
        val client =
            get<OkHttpClient>().newBuilder()
                .addInterceptor(queryParamInterceptor("key", BuildConfig.PIXABAY_API_KEY))
                .build()
        retrofit(PIXABAY_BASE_URL, client, get()).create(PixabayApi::class.java)
    }

    single {
        val client =
            get<OkHttpClient>().newBuilder()
                .addInterceptor(headerInterceptor("Authorization", "Client-ID ${BuildConfig.UNSPLASH_ACCESS_KEY}"))
                .build()
        retrofit(UNSPLASH_BASE_URL, client, get()).create(UnsplashApi::class.java)
    }

    single { retrofit(OPENVERSE_BASE_URL, get(), get()).create(OpenverseApi::class.java) }

    single {
        val client =
            get<OkHttpClient>().newBuilder()
                .addInterceptor(headerInterceptor("Authorization", "DeepL-Auth-Key ${BuildConfig.DEEPL_API_KEY}"))
                .build()
        retrofit(DEEPL_BASE_URL, client, get()).create(DeepLApi::class.java)
    }

    single {
        val client =
            get<OkHttpClient>().newBuilder()
                .addInterceptor(headerInterceptor("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}"))
                .build()
        retrofit(OPENAI_BASE_URL, client, get()).create(OpenAiApi::class.java)
    }

    single<SentenceGenerator> { OpenAiSentenceGenerator(get()) }
    single<FillwordGenerator> { OpenAiFillwordGenerator(get()) }

    single { retrofit(MYMEMORY_BASE_URL, get(), get()).create(MyMemoryApi::class.java) }

    factory<List<Translator>>(translatorChainQualifier) {
        buildList {
            add(get<CorpusTranslatorImpl>())
            if (hasDeepLKey) add(get<DeepLTranslator>())
            add(get<MyMemoryTranslator>())
        }
    }

    factoryOf(::PexelsImageSource)
    factoryOf(::PixabayImageSource)
    factoryOf(::UnsplashImageSource)
    factoryOf(::OpenverseImageSource)
    factoryOf(::DeepLTranslator)
    factoryOf(::MyMemoryTranslator)
}

val hasDeepLKey: Boolean get() = BuildConfig.DEEPL_API_KEY.isNotBlank()
