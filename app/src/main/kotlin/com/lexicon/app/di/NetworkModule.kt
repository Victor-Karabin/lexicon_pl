package com.lexicon.app.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.lexicon.BuildConfig
import com.lexicon.data.remote.image.OpenverseApi
import com.lexicon.data.remote.image.PexelsApi
import com.lexicon.data.remote.image.PixabayApi
import com.lexicon.data.remote.image.UnsplashApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

private const val PEXELS_BASE_URL = "https://api.pexels.com/"
private const val PIXABAY_BASE_URL = "https://pixabay.com/"
private const val UNSPLASH_BASE_URL = "https://api.unsplash.com/"
private const val OPENVERSE_BASE_URL = "https://api.openverse.org/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideBaseOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

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

    @Provides
    @Singleton
    fun providePexelsApi(
        baseClient: OkHttpClient,
        json: Json,
    ): PexelsApi {
        val client =
            baseClient.newBuilder()
                .addInterceptor(headerInterceptor("Authorization", BuildConfig.PEXELS_API_KEY))
                .build()
        return retrofit(PEXELS_BASE_URL, client, json).create(PexelsApi::class.java)
    }

    @Provides
    @Singleton
    fun providePixabayApi(
        baseClient: OkHttpClient,
        json: Json,
    ): PixabayApi {
        val client =
            baseClient.newBuilder()
                .addInterceptor(queryParamInterceptor("key", BuildConfig.PIXABAY_API_KEY))
                .build()
        return retrofit(PIXABAY_BASE_URL, client, json).create(PixabayApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUnsplashApi(
        baseClient: OkHttpClient,
        json: Json,
    ): UnsplashApi {
        val client =
            baseClient.newBuilder()
                .addInterceptor(headerInterceptor("Authorization", "Client-ID ${BuildConfig.UNSPLASH_ACCESS_KEY}"))
                .build()
        return retrofit(UNSPLASH_BASE_URL, client, json).create(UnsplashApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenverseApi(
        baseClient: OkHttpClient,
        json: Json,
    ): OpenverseApi = retrofit(OPENVERSE_BASE_URL, baseClient, json).create(OpenverseApi::class.java)
}
