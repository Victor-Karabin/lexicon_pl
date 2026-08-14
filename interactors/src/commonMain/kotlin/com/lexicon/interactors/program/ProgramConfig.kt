package com.lexicon.interactors.program

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ProgramId(val value: String)

/**
 * Everything a program is, as configuration.
 *
 * The engine reads this and nothing else — no program is special-cased anywhere, so
 * a new one is a new JSON file. Every list defaults to empty and every number to
 * something usable, because a program that only sets a goal and a daily plan should
 * still run.
 */
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

// ---------------------------------------------------------------- goals

enum class TargetType {
    /** Words mastered, by the review schedule's reckoning. */
    VOCABULARY,
    LESSONS,

    /** Minutes studied. */
    TIME,

    /** Steps answered. */
    EXERCISES,

    /** Consecutive days studied. */
    STREAK,

    /** Percentage, 0-100. */
    RETENTION,
}

@Serializable
data class ProgramGoal(
    val id: String,
    val type: TargetType,
    val target: Int,
    /** Days from enrolment; absent means no deadline. */
    val deadlineDays: Int? = null,
    val required: Boolean = true,
)

// ---------------------------------------------------------------- scope

enum class ScopeSourceType { PRESET, FAVOURITES, CEFR_LEVEL, LESSON, ALL }

@Serializable
data class ScopeSource(
    val type: ScopeSourceType,
    /** Preset id, CEFR band or lesson id, depending on [type]. */
    val value: String = "",
)

enum class ScopeOrdering { AS_LISTED, FREQUENCY, DIFFICULTY, ALPHABETICAL, RANDOM }

@Serializable
data class VocabularyScope(
    val include: List<ScopeSource> = emptyList(),
    val exclude: List<ScopeSource> = emptyList(),
    val maxWords: Int? = null,
    val ordering: ScopeOrdering = ScopeOrdering.FREQUENCY,
)

// ---------------------------------------------------------------- strategy

enum class LearningStrategy { NEW_FIRST, REVIEWS_FIRST, MIXED, TOPIC_BY_TOPIC, ADAPTIVE }

// ---------------------------------------------------------------- daily plan

enum class ActivityType { LEARN, REVIEW, PRONOUNCE, LISTEN, WRITE, MIXED, CHALLENGE }

/**
 * What to do, not how. [trainings] names the trainings that can satisfy it; the
 * engine picks among them, which is what keeps a program independent of any one.
 */
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

/**
 * A day's work. [weekend] overrides the weekday shape where a program wants one —
 * the spec's "review only, longer practice" — and falls back to the weekday plan
 * when absent.
 */
@Serializable
data class DailyPlanConfig(
    val newWords: Int = 0,
    val reviewWords: Int = 0,
    val minMinutes: Int = 0,
    val maxWords: Int? = null,
    val activities: List<ActivityConfig> = emptyList(),
    val weekend: WeekendPlanConfig? = null,
)

@Serializable
data class WeekendPlanConfig(
    val newWords: Int = 0,
    val reviewWords: Int = 0,
    val minMinutes: Int = 0,
    val activities: List<ActivityConfig> = emptyList(),
)

// ---------------------------------------------------------------- milestones

@Serializable
data class MilestoneConfig(
    val id: String,
    val title: Map<String, String> = emptyMap(),
    val conditions: List<ProgramCondition> = emptyList(),
)

/** One measurable requirement, shared by milestones and completion rules. */
@Serializable
data class ProgramCondition(
    val type: TargetType,
    val target: Int,
)

// ---------------------------------------------------------------- progress

/**
 * How much each metric counts toward the single figure.
 *
 * Weights are validated at build time to sum to 100, so nothing here has to
 * normalise or apologise for a program whose numbers do not add up.
 */
@Serializable
data class ProgressWeights(
    val vocabulary: Int = 100,
    val milestones: Int = 0,
    val retention: Int = 0,
    val consistency: Int = 0,
    val studyTime: Int = 0,
    val accuracy: Int = 0,
)

// ---------------------------------------------------------------- review

@Serializable
data class ReviewStrategyConfig(
    val dailyLimit: Int = 50,
    val firstIntervalDays: Int = 1,
    val secondIntervalDays: Int = 6,
    val minIntervalDays: Int = 1,
    val maxIntervalDays: Int = 365,
    val minimumEase: Double = 1.3,
    val masteredIntervalDays: Int = 21,
    /** Words that lapsed come back before those merely due. */
    val failedFirst: Boolean = true,
)

// ---------------------------------------------------------------- adaptation

enum class AdaptationTrigger {
    HIGH_ACCURACY,
    LOW_ACCURACY,
    MISSED_DAY,
    MISSED_WEEK,
    FAST_PROGRESS,
    SLOW_PROGRESS,
    REVIEW_BACKLOG,
}

enum class AdaptationAction {
    INCREASE_NEW_WORDS,
    DECREASE_NEW_WORDS,
    INCREASE_REVIEWS,
    PAUSE_NEW_WORDS,
    EXTEND_DURATION,
    REPEAT_MILESTONE,
}

@Serializable
data class AdaptationRule(
    val id: String,
    val trigger: AdaptationTrigger,
    /** What the trigger compares against: a percentage, a day count, a backlog size. */
    val threshold: Int,
    val action: AdaptationAction,
    /** How much the action moves things, in whatever unit the action implies. */
    val amount: Int = 1,
)

// ---------------------------------------------------------------- rewards

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

// ---------------------------------------------------------------- completion

@Serializable
data class CompletionRules(
    val conditions: List<ProgramCondition> = emptyList(),
    val requireAllGoals: Boolean = true,
    val requireAllMilestones: Boolean = true,
)
