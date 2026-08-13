package com.lexicon.app.di

import androidx.room.Room
import com.lexicon.data.local.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val DATABASE_NAME = "lexicon.db"

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }
    factory { get<AppDatabase>().wordDao() }
    factory { get<AppDatabase>().trainingResultDao() }
    factory { get<AppDatabase>().imageUrlCacheDao() }
    factory { get<AppDatabase>().presetDao() }
    factory { get<AppDatabase>().courseDao() }
}
