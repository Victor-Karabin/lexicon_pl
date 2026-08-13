@file:OptIn(ExperimentalForeignApi::class)

package com.lexicon.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

/**
 * The catalogues are copied into the app bundle as resources (see the iOS target's
 * Copy Bundle Resources phase), so they are looked up by name and extension rather
 * than by the "name.json" path Android's AssetManager takes.
 */
actual class AssetReader {
    actual fun readText(fileName: String): String {
        val name = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        val path =
            NSBundle.mainBundle.pathForResource(name, extension)
                ?: error("$fileName is not in the app bundle")
        return requireNotNull(
            NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null),
        ) { "$fileName could not be read as UTF-8" }
    }
}
