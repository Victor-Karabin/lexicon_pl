package com.lexicon.interactors.course

data class LessonSummary(
    val id: LessonId,
    val number: Int,
    val title: String,
    val wordCount: Int,
    val isCompleted: Boolean,
    val isUnlocked: Boolean,
)
