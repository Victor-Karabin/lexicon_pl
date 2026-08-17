package com.lexicon.data.local

import kotlinx.serialization.Serializable

@Serializable
data class PassageCatalogAsset(
    val passages: List<PassageAsset> = emptyList(),
)

@Serializable
data class PassageAsset(
    val id: String,
    val title: String = "",
    val cefr: String = "A1",
    val text: String = "",
    val keyWords: List<String> = emptyList(),
)
