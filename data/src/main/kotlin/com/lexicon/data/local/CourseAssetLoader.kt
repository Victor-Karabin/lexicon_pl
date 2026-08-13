package com.lexicon.data.local

import android.content.Context
import kotlinx.serialization.json.Json

private const val COURSE_ASSET_PATH = "course_krok.json"

class CourseAssetLoader(
    private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): CourseCatalogAsset = json.decodeFromString(readAsset())

    fun fingerprint(): String = readAsset().let { "${it.length}:${it.hashCode()}" }

    private fun readAsset(): String = context.assets.open(COURSE_ASSET_PATH).bufferedReader().use { it.readText() }
}
