package com.lexicon.interactors.training

const val SINGLE_ANSWER_MASTERY_STREAK = 1

data class DrillCourseProgress(
    val items: Int,
    val total: Int,
    val mastered: Int,
    val attempted: Int,
) {
    val fraction: Float get() = if (total == 0) 0f else mastered.toFloat() / total

    val isComplete: Boolean get() = total > 0 && mastered == total
}
