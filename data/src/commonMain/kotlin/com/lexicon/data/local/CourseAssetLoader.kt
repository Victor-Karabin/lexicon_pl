package com.lexicon.data.local

import kotlinx.serialization.json.Json

private const val COURSE_ASSET_PATH = "course_krok.json"

class CourseAssetLoader(
    private val assets: AssetReader,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): CourseCatalogAsset = readAsset()?.let { json.decodeFromString<CourseCatalogAsset>(it) } ?: CourseCatalogAsset()

    fun fingerprint(): String = readAsset().orEmpty().assetFingerprint()

    private fun readAsset(): String? = runCatching { assets.readText(COURSE_ASSET_PATH) }.getOrNull()
}
