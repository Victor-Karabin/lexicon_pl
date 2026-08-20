@file:OptIn(ExperimentalForeignApi::class)

package com.lexicon.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class AppDatabaseBuilderFactory {
    actual fun create(): RoomDatabase.Builder<AppDatabase> = Room.databaseBuilder<AppDatabase>(name = databasePath())

    private fun databasePath(): String {
        val documents =
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            )
        return requireNotNull(documents?.path) { "No documents directory to open $DATABASE_NAME in" } + "/" + DATABASE_NAME
    }
}
