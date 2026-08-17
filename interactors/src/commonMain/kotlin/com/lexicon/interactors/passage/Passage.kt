package com.lexicon.interactors.passage

import kotlinx.collections.immutable.ImmutableList

data class Passage(
    val level: String,
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

val CEFR_ORDER = listOf("A1", "A2", "B1", "B2", "C1", "C2")

fun sentenceCountFor(level: String): IntRange {
    val step = CEFR_ORDER.indexOf(level.uppercase()).coerceAtLeast(0)
    return (3 + step)..(4 + step)
}
