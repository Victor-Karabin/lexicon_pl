package com.lexicon.data.local

import kotlinx.serialization.json.Json

private const val PROGRAM_ASSET_PATH = "programs.json"

class ProgramAssetLoader(
    private val assets: AssetReader,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): ProgramCatalogAsset = json.decodeFromString(readAsset())

    fun fingerprint(): String = readAsset().let { "${it.length}:${it.hashCode()}" }

    private fun readAsset(): String = assets.readText(PROGRAM_ASSET_PATH)
}
