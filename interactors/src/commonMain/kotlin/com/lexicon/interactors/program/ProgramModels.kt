package com.lexicon.interactors.program

import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.VocabularyId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class ProgramDifficulty { BEGINNER, INTERMEDIATE, ADVANCED }

enum class ProgramVisibility { PUBLIC, PRIVATE }

/** A program as it is listed: what it is, plus everything it is configured to do. */
data class Program(
    val id: ProgramId,
    val level: String,
    val order: Int,
    val title: LocalizedText,
    val description: LocalizedText,
    val difficulty: ProgramDifficulty,
    val estimatedDays: Int,
    val visibility: ProgramVisibility,
    val config: ProgramConfig,
)

enum class EnrolmentStatus { ACTIVE, COMPLETED, ABANDONED }

data class ProgramEnrolment(
    val programId: ProgramId,
    val startedAtEpochDay: Long,
    val status: EnrolmentStatus,
    val completedAtEpochDay: Long? = null,
)

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

/** A generated day, as stored. */
data class DailyPlan(
    val programId: ProgramId,
    val epochDay: Long,
    val activities: ImmutableList<PlannedActivity> = persistentListOf(),
    /** Adaptation rules that shaped this day, by id, so the change can be explained. */
    val appliedRules: ImmutableList<String> = persistentListOf(),
) {
    val requiredActivities: List<PlannedActivity> get() = activities.filter { it.required }

    val isComplete: Boolean get() = requiredActivities.isNotEmpty() && requiredActivities.all { it.isComplete }
}

/** One metric's contribution, kept apart so the UI can show the breakdown. */
data class ProgressMetric(
    val type: ProgressMetricType,
    val current: Int,
    val target: Int,
    val weight: Int,
) {
    val fraction: Double get() = if (target <= 0) 1.0 else (current.toDouble() / target).coerceIn(0.0, 1.0)
}

enum class ProgressMetricType { VOCABULARY, MILESTONES, CONSISTENCY, STUDY_TIME, ACCURACY }

data class ProgramProgress(
    val programId: ProgramId,
    val metrics: ImmutableList<ProgressMetric> = persistentListOf(),
) {
    /** The weighted whole, 0.0 to 1.0. Weights sum to 100, checked when the asset is built. */
    val overall: Double
        get() {
            val totalWeight = metrics.sumOf { it.weight }
            if (totalWeight == 0) return 0.0
            return metrics.sumOf { it.fraction * it.weight } / totalWeight
        }
}

data class MilestoneState(
    val id: String,
    val title: LocalizedText,
    val achievedAtEpochDay: Long?,
    val conditions: ImmutableList<ConditionState> = persistentListOf(),
) {
    val isAchieved: Boolean get() = achievedAtEpochDay != null
}

data class ConditionState(
    val type: TargetType,
    val current: Int,
    val target: Int,
) {
    val isMet: Boolean get() = current >= target
}

data class GrantedReward(
    val id: String,
    val type: RewardType,
    val title: LocalizedText,
    val grantedAtEpochDay: Long,
)
