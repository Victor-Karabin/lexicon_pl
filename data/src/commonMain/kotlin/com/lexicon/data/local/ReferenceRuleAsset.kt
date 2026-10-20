package com.lexicon.data.local

import com.lexicon.model.rules.ReferenceRule
import com.lexicon.model.rules.ReferenceRuleId
import com.lexicon.model.rules.ReferenceSection
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable

const val REFERENCE_RULES_ASSET_PATH = "rules.json"

@Serializable
data class ReferenceRulesAsset(
    val rules: List<ReferenceRuleAsset> = emptyList(),
)

@Serializable
data class ReferenceRuleAsset(
    val id: String,
    val title: String,
    val sections: List<ReferenceSectionAsset> = emptyList(),
)

@Serializable
data class ReferenceSectionAsset(
    val title: String,
    val intro: List<String> = emptyList(),
    val tables: List<TableAsset> = emptyList(),
    val notes: List<String> = emptyList(),
)

fun parseReferenceRules(raw: String): List<ReferenceRule> = lessonJson.decodeFromString<ReferenceRulesAsset>(raw).rules.map { it.toModel() }

private fun ReferenceRuleAsset.toModel(): ReferenceRule =
    ReferenceRule(
        id = ReferenceRuleId(id),
        title = title,
        sections = sections.map { section ->
            ReferenceSection(
                title = section.title,
                intro = section.intro.toImmutableList(),
                tables = section.tables.map { it.toModel() }.toImmutableList(),
                notes = section.notes.toImmutableList(),
            )
        }.toImmutableList(),
    )
