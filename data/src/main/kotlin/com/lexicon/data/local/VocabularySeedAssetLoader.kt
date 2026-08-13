package com.lexicon.data.local

import android.content.Context
import kotlinx.serialization.json.Json

private const val VOCABULARY_ASSET_PATH = "vocabulary_pl.json"

class VocabularySeedAssetLoader(
    private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<WordEntity> = json.decodeFromString<List<VocabularySeedItem>>(readAsset()).map { it.toEntity() }

    fun fingerprint(): String = readAsset().let { "${it.length}:${it.hashCode()}" }

    private fun readAsset(): String = context.assets.open(VOCABULARY_ASSET_PATH).bufferedReader().use { it.readText() }
}
