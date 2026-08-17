package com.lexicon.interactors.program

import com.lexicon.interactors.presets.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * One activity as it stands today: what to do, how much of it, and how much is done.
 *
 * [wordIds] is fixed when the day is generated rather than chosen when the activity
 * is opened, so the work does not shift under the learner between looking at the day
 * and starting it.
 */
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
