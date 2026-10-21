package com.lexicon.data.local

internal fun String.assetFingerprint(): String = "$length:${hashCode()}"
