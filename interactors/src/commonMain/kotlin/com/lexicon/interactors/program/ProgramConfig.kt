package com.lexicon.interactors.program

import com.lexicon.model.program.ActivityType
import com.lexicon.model.program.AdaptationAction
import com.lexicon.model.program.AdaptationTrigger
import com.lexicon.model.program.LearningStrategy
import com.lexicon.model.program.ScopeOrdering
import com.lexicon.model.program.ScopeSourceType
import com.lexicon.model.program.TargetType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProgramConfig(
    val goals: List<ProgramGoal> = emptyList(),
    val scope: VocabularyScope = VocabularyScope(),
    val strategy: LearningStrategy = LearningStrategy.MIXED,
    val dailyPlan: DailyPlanConfig = DailyPlanConfig(),
    val milestones: List<MilestoneConfig> = emptyList(),
    val progress: ProgressWeights = ProgressWeights(),
    val review: ReviewStrategyConfig = ReviewStrategyConfig(),
    val adaptation: List<AdaptationRule> = emptyList(),
    val rewards: List<RewardConfig> = emptyList(),
    val completion: CompletionRules = CompletionRules(),
)

@Serializable
data class ProgramGoal(
    val id: String,
    val type: TargetType,
    val target: Int,
    val deadlineDays: Int? = null,
    val required: Boolean = true,
)

@Serializable
data class ScopeSource(
    val type: ScopeSourceType,
    val value: String = "",
)

@Serializable
data class VocabularyScope(
    val include: List<ScopeSource> = emptyList(),
    val exclude: List<ScopeSource> = emptyList(),
    val maxWords: Int? = null,
    val ordering: ScopeOrdering = ScopeOrdering.FREQUENCY,
)

@Serializable
data class ActivityConfig(
    val id: String,
    val type: ActivityType,
    val target: Int = 1,
    val required: Boolean = true,
    val estimatedMinutes: Int = 5,
    val priority: Int = 0,
    val trainings: List<String> = emptyList(),
)

@Serializable
data class DailyPlanConfig(
    val newWords: Int = 0,
    val reviewWords: Int = 0,
    val minMinutes: Int = 0,
    val maxWords: Int? = null,
    val activities: List<ActivityConfig> = emptyList(),
    val weekend: WeekendPlanConfig? = null,
    val queue: List<String> = emptyList(),
)

val DailyPlanConfig.trainingsADay: Int get() = queue.size

@Serializable
data class WeekendPlanConfig(
    val newWords: Int = 0,
    val reviewWords: Int = 0,
    val minMinutes: Int = 0,
    val activities: List<ActivityConfig> = emptyList(),
)

@Serializable
data class MilestoneConfig(
    val id: String,
    val title: Map<String, String> = emptyMap(),
    val conditions: List<ProgramCondition> = emptyList(),
)

@Serializable
data class ProgramCondition(
    val type: TargetType,
    val target: Int,
)

@Serializable
data class ProgressWeights(
    val vocabulary: Int = 100,
    val milestones: Int = 0,
    val consistency: Int = 0,
    val studyTime: Int = 0,
    val accuracy: Int = 0,
)

@Serializable
data class ReviewStrategyConfig(
    val dailyLimit: Int = 50,
    val firstIntervalDays: Int = 1,
    val secondIntervalDays: Int = 6,
    val minIntervalDays: Int = 1,
    val maxIntervalDays: Int = 365,
    val minimumEase: Double = 1.3,
    val masteredIntervalDays: Int = 21,
    val failedFirst: Boolean = true,
)

@Serializable
data class AdaptationRule(
    val id: String,
    val trigger: AdaptationTrigger,
    val threshold: Int,
    val action: AdaptationAction,
    val amount: Int = 1,
)

enum class RewardTrigger { MILESTONE, DAILY_GOAL, WEEKLY_GOAL, PROGRAM_COMPLETED, STREAK, ACCURACY, STUDY_TIME }

enum class RewardType { BADGE, XP, CERTIFICATE, TROPHY }

@Serializable
data class RewardConfig(
    val id: String,
    val trigger: RewardTrigger,
    val threshold: Int = 0,
    val type: RewardType = RewardType.BADGE,
    val title: Map<String, String> = emptyMap(),
    val value: Int = 0,
    @SerialName("icon") val iconName: String? = null,
)

@Serializable
data class CompletionRules(
    val conditions: List<ProgramCondition> = emptyList(),
    val requireAllGoals: Boolean = true,
    val requireAllMilestones: Boolean = true,
)
