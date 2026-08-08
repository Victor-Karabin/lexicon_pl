package com.lexicon.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val VOCABULARY_ASSET_PATH = "vocabulary_pl.json"

class VocabularySeedAssetLoader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        fun load(): List<WordEntity> = json.decodeFromString<List<VocabularySeedItem>>(readAsset()).map { it.toEntity() }

        fun fingerprint(): String = readAsset().let { "${it.length}:${it.hashCode()}" }

        private fun readAsset(): String = context.assets.open(VOCABULARY_ASSET_PATH).bufferedReader().use { it.readText() }
    }
