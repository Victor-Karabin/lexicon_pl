package com.lexicon.data.local

import com.lexicon.boundary.AppVersionProvider
import platform.Foundation.NSBundle

fun iosAppVersionProvider(): AppVersionProvider =
    AppVersionProvider {
        val build = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
        build?.toIntOrNull() ?: 0
    }
