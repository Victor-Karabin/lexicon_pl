package com.lexicon.data.repository

import com.lexicon.boundary.PassageBoundary
import com.lexicon.boundary.PassageRepository
import com.lexicon.data.local.AssetReader
import com.lexicon.data.local.PassageCatalogAsset
import kotlinx.serialization.json.Json

private const val PASSAGES_ASSET = "passages.json"

class PassageRepositoryImpl(
    private val assets: AssetReader,
) : PassageRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private var cached: List<PassageBoundary>? = null

    override suspend fun passages(): List<PassageBoundary> =
        cached ?: runCatching {
            json
                .decodeFromString(PassageCatalogAsset.serializer(), assets.readText(PASSAGES_ASSET))
                .passages
                .map {
                    PassageBoundary(
                        id = it.id,
                        title = it.title,
                        cefr = it.cefr,
                        text = it.text,
                        keyWords = it.keyWords,
                    )
                }
        }.getOrDefault(emptyList()).also { cached = it }
}
