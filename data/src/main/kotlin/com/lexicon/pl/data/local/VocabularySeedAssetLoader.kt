package com.lexicon.pl.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val VOCABULARY_ASSET_PATH = "vocabulary_pl.json"

/** Reads the bundled mock vocabulary from assets. Replace with a real source once one exists. */
class VocabularySeedAssetLoader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        fun load(): List<WordEntity> {
            val raw = context.assets.open(VOCABULARY_ASSET_PATH).bufferedReader().use { it.readText() }
            return json.decodeFromString<List<VocabularySeedItem>>(raw).map { it.toEntity() }
        }
    }
