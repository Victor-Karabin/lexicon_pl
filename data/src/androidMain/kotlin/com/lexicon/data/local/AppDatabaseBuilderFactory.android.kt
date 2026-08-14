package com.lexicon.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class AppDatabaseBuilderFactory(
    private val context: Context,
) {
    actual fun create(): RoomDatabase.Builder<AppDatabase> = Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
}
