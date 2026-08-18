package com.lexicon.interactors.passage

import kotlinx.collections.immutable.ImmutableList

data class Passage(
    val level: String,
    val sentences: ImmutableList<PassageSentence>,
) {
    val spoken: List<PassageSegment> get() = sentences.flatMap { it.segments }

    val gaps: List<PassageSegment.Gap> get() = spoken.filterIsInstance<PassageSegment.Gap>()
}

data class PassageSentence(
    val segments: ImmutableList<PassageSegment>,
)

sealed interface PassageSegment {
    data class Text(val text: String) : PassageSegment

    /**
     * A blank to fill in.
     *
     * [answer] is the form as it appears in the sentence, which is what the learner has
     * to type; [word] is the favourite it was inflected from, which is the only form the
     * vocabulary can be looked up by.
     */
    data class Gap(
        val answer: String,
        val word: String,
    ) : PassageSegment
}

val CEFR_ORDER = listOf("A1", "A2", "B1", "B2", "C1", "C2")
