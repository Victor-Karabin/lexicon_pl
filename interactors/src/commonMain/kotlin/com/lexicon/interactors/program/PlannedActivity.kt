package com.lexicon.interactors.program

import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class PlannedActivity(
    val id: String,
    val type: ActivityType,
    val target: Int,
    val completed: Int,
    val required: Boolean,
    val estimatedMinutes: Int,
    val training: String,
    val wordIds: ImmutableList<VocabularyId> = persistentListOf(),
) {
    val isComplete: Boolean get() = completed >= target
}
