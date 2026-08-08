package com.lexicon.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val PRESET_ASSET_PATH = "vocabulary_presets.json"

class VocabularyPresetAssetLoader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        fun load(): VocabularyPresetCatalogAsset = json.decodeFromString(readAsset())

        fun fingerprint(): String = readAsset().let { "${it.length}:${it.hashCode()}" }

        private fun readAsset(): String = context.assets.open(PRESET_ASSET_PATH).bufferedReader().use { it.readText() }
    }
