package com.lexicon.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class ProgramCatalogAsset(
    val programs: List<ProgramAsset> = emptyList(),
)

/**
 * A program as authored.
 *
 * The metadata is flat and becomes columns. [config] stays a [JsonElement]: what a
 * program is configured to do is the engine's business, and the data layer has no
 * reason to know the shape of it — it carries the block through intact.
 */
@Serializable
data class ProgramAsset(
    val id: String,
    val level: String,
    val order: Int = 0,
    val title: Map<String, String> = emptyMap(),
    val description: Map<String, String> = emptyMap(),
    val difficulty: String = "BEGINNER",
    val estimatedDays: Int = 0,
    val visibility: String = "PUBLIC",
    val config: JsonElement = JsonObject(emptyMap()),
)
