package com.lexicon.data.local

import kotlinx.serialization.json.Json

private const val PRESET_ASSET_PATH = "vocabulary_presets.json"

class VocabularyPresetAssetLoader(
    private val assets: AssetReader,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): VocabularyPresetCatalogAsset = json.decodeFromString(readAsset())

    fun fingerprint(): String = readAsset().let { "${it.length}:${it.hashCode()}" }

    private fun readAsset(): String = assets.readText(PRESET_ASSET_PATH)
}
