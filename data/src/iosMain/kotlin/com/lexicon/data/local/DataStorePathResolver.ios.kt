@file:OptIn(ExperimentalForeignApi::class)

package com.lexicon.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private const val DATA_STORE_FILE_SUFFIX = ".preferences_pb"

actual class DataStorePathResolver {
    actual fun path(name: String): String {
        val documents =
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = null,
            )
        return requireNotNull(documents?.path) { "No documents directory to open $name in" } + "/" + name + DATA_STORE_FILE_SUFFIX
    }
}
