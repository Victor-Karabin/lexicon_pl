package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "programs", indices = [Index("level")])
data class ProgramEntity(
    @PrimaryKey val id: String,
    val level: String,
    val sortOrder: Int,
    val titleJson: String,
    val descriptionJson: String,
    val difficulty: String,
    val estimatedDays: Int,
    val visibility: String,
    val configJson: String,
    val isUserCreated: Boolean = true,
)

@Entity(tableName = "program_enrolment")
data class ProgramEnrolmentEntity(
    @PrimaryKey val programId: String,
    val startedAtEpochDay: Long,
    val status: String,
    val completedAtEpochDay: Long? = null,
)

@Entity(tableName = "program_day", primaryKeys = ["programId", "epochDay"], indices = [Index("programId")])
data class ProgramDayEntity(
    val programId: String,
    val epochDay: Long,
    val activitiesJson: String,
    val appliedRulesJson: String,
    val isComplete: Boolean,
)

@Entity(tableName = "program_milestone", primaryKeys = ["programId", "milestoneId"], indices = [Index("programId")])
data class ProgramMilestoneEntity(
    val programId: String,
    val milestoneId: String,
    val achievedAtEpochDay: Long,
)

@Entity(tableName = "program_reward", primaryKeys = ["programId", "rewardId"], indices = [Index("programId")])
data class ProgramRewardEntity(
    val programId: String,
    val rewardId: String,
    val grantedAtEpochDay: Long,
)
