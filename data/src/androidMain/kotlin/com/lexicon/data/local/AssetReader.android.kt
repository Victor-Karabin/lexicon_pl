package com.lexicon.data.local

import android.content.Context

actual class AssetReader(
    private val context: Context,
) {
    actual fun readText(fileName: String): String = context.assets.open(fileName).bufferedReader().use { it.readText() }
}
