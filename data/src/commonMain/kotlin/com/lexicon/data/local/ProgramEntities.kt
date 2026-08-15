package com.lexicon.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A program's configuration, shipped whole and read whole.
 *
 * Only the fields worth querying or listing are columns; the eleven configured
 * sections live in [configJson]. Normalising them would be ten joins to reassemble
 * something never read in parts, and [titleJson] already sets the precedent.
 */
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
    /**
     * Always true now: every program is written by the learner, since nothing ships
     * a catalogue any more. Kept because dropping a column is a schema change, and
     * this database takes the destructive fallback — it would cost the learner
     * everything to remove a flag that costs nothing to leave.
     */
    val isUserCreated: Boolean = true,
)

/** That the learner is working through a program, and since when. */
@Entity(tableName = "program_enrolment")
data class ProgramEnrolmentEntity(
    @PrimaryKey val programId: String,
    val startedAtEpochDay: Long,
    val status: String,
    val completedAtEpochDay: Long? = null,
)

/**
 * One generated day.
 *
 * The plan is stored rather than regenerated on every open: the day's work would
 * otherwise shift as the schedule moved beneath it, and adaptation would leave no
 * trace of what it did.
 */
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
