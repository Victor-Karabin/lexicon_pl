package com.lexicon.model.rules

import com.lexicon.model.course.LessonTable
import kotlinx.collections.immutable.ImmutableList

data class ReferenceRuleId(val value: String)

val CASES_RULE = ReferenceRuleId("przypadki")

data class ReferenceRule(
    val id: ReferenceRuleId,
    val title: String,
    val sections: ImmutableList<ReferenceSection>,
)

data class ReferenceSection(
    val title: String,
    val intro: ImmutableList<String>,
    val tables: ImmutableList<LessonTable>,
    val notes: ImmutableList<String>,
)
