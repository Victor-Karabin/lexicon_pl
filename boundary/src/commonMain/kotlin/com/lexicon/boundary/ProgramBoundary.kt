package com.lexicon.boundary

data class ProgramBoundary(
    val id: String,
    val level: String,
    val order: Int,
    val title: Map<String, String>,
    val description: Map<String, String>,
    val difficulty: String,
    val estimatedDays: Int,
    val visibility: String,
    val configJson: String,
    val isUserCreated: Boolean = true,
)

data class ProgramEnrolmentBoundary(
    val programId: String,
    val startedAtEpochDay: Long,
    val status: String,
    val completedAtEpochDay: Long?,
)

data class ProgramDayBoundary(
    val programId: String,
    val epochDay: Long,
    val activitiesJson: String,
    val appliedRulesJson: String,
    val isComplete: Boolean,
)

data class ProgramMilestoneBoundary(
    val programId: String,
    val milestoneId: String,
    val achievedAtEpochDay: Long,
)

data class ProgramRewardBoundary(
    val programId: String,
    val rewardId: String,
    val grantedAtEpochDay: Long,
)
