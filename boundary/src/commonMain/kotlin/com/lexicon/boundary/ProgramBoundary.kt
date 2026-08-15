package com.lexicon.boundary

/**
 * A program crossing the data boundary.
 *
 * [configJson] is deliberately opaque here. The data layer stores and returns a
 * program's configuration without knowing what any of it means; only the domain
 * layer, which owns the engine that reads it, parses it into a shape.
 */
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
