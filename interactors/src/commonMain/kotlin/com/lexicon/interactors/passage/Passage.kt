package com.lexicon.interactors.passage

import kotlinx.collections.immutable.ImmutableList

data class Passage(
    val id: String,
    val title: String,
    val cefr: String,
    val segments: ImmutableList<PassageSegment>,
) {
    val plainText: String
        get() = segments.joinToString("") {
            when (it) {
                is PassageSegment.Text -> it.text
                is PassageSegment.Gap -> it.answer
            }
        }

    val gaps: List<PassageSegment.Gap> get() = segments.filterIsInstance<PassageSegment.Gap>()
}

sealed interface PassageSegment {
    data class Text(val text: String) : PassageSegment

    data class Gap(val answer: String) : PassageSegment
}
