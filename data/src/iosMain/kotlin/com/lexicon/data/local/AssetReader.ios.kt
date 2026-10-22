@file:OptIn(ExperimentalForeignApi::class)

package com.lexicon.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

actual class AssetReader {
    actual fun readText(fileName: String): String {
        val path = pathOf(fileName) ?: error("$fileName is not in the app bundle")
        return requireNotNull(
            NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null),
        ) { "$fileName could not be read as UTF-8" }
    }

    actual fun exists(fileName: String): Boolean = pathOf(fileName) != null

    private fun pathOf(fileName: String): String? {
        val name = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        return NSBundle.mainBundle.pathForResource(name, extension)
    }
}
