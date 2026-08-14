package com.lexicon.data.local

import kotlinx.serialization.json.Json

private const val VOCABULARY_ASSET_PATH = "vocabulary_pl.json"

class VocabularySeedAssetLoader(
    private val assets: AssetReader,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<WordEntity> = json.decodeFromString<List<VocabularySeedItem>>(readAsset()).map { it.toEntity() }

    fun fingerprint(): String = readAsset().let { "${it.length}:${it.hashCode()}" }

    private fun readAsset(): String = assets.readText(VOCABULARY_ASSET_PATH)
}
