package com.lexicon.data.local

import com.lexicon.boundary.VerbConjugationBoundary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val CONJUGATION_ASSET_PATH = "conjugations.json"

private const val ALTERNATIVE_SEPARATOR = ';'

private val UNUSABLE = setOf("", "-", "n/a", "na")

@Serializable
data class ConjugationEntryAsset(
    @SerialName("bezokolicznik") val infinitive: String = "",
    @SerialName("translation") val translation: String = "",
    @SerialName("ja") val first: String = "",
    @SerialName("ty") val second: String = "",
    @SerialName("on/ona/ono") val thirdSingular: String = "",
    @SerialName("my") val firstPlural: String = "",
    @SerialName("wy") val secondPlural: String = "",
    @SerialName("oni/one") val thirdPlural: String = "",
)

class ConjugationAssetLoader(
    private val assets: AssetReader,
) {
    fun load(): List<VerbConjugationBoundary> = parseConjugations(readAsset())

    fun fingerprint(): String = readAsset().let { "${'$'}{it.length}:${'$'}{it.hashCode()}" }

    private fun readAsset(): String = assets.readText(CONJUGATION_ASSET_PATH)
}

private val conjugationJson = Json { ignoreUnknownKeys = true }

fun parseConjugations(raw: String): List<VerbConjugationBoundary> =
    conjugationJson
        .decodeFromString<List<ConjugationEntryAsset>>(raw)
        .mapNotNull { it.toBoundary() }

private fun ConjugationEntryAsset.toBoundary(): VerbConjugationBoundary? {
    val infinitive = infinitive.trim()
    if (infinitive.isEmpty()) return null

    val forms = buildMap {
        putUsable(PERSON_JA, first)
        putUsable(PERSON_TY, second)
        putUsable(PERSON_ON, thirdSingular)
        putUsable(PERSON_MY, firstPlural)
        putUsable(PERSON_WY, secondPlural)
        putUsable(PERSON_ONI, thirdPlural)
    }

    return VerbConjugationBoundary(
        infinitive = infinitive,
        translation = translation.trim().takeIf { it.isNotEmpty() },
        forms = forms,
    )
}

private fun MutableMap<String, List<String>>.putUsable(
    person: String,
    raw: String,
) {
    val alternatives = raw
        .split(ALTERNATIVE_SEPARATOR)
        .map { it.trim() }
        .filter { it.lowercase() !in UNUSABLE }

    if (alternatives.isNotEmpty()) put(person, alternatives)
}

const val PERSON_JA = "ja"
const val PERSON_TY = "ty"
const val PERSON_ON = "on/ona/ono"
const val PERSON_MY = "my"
const val PERSON_WY = "wy"
const val PERSON_ONI = "oni/one"
