package com.lexicon.data.local

import kotlinx.serialization.json.Json

private const val COURSE_ASSET_PATH = "course_krok.json"

class CourseAssetLoader(
    private val assets: AssetReader,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): CourseCatalogAsset = json.decodeFromString(readAsset())

    fun fingerprint(): String = readAsset().let { "${it.length}:${it.hashCode()}" }

    private fun readAsset(): String = assets.readText(COURSE_ASSET_PATH)
}
