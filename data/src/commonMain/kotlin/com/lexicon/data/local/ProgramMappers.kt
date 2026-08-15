package com.lexicon.data.local

import com.lexicon.boundary.ProgramBoundary
import com.lexicon.boundary.ProgramDayBoundary
import com.lexicon.boundary.ProgramEnrolmentBoundary
import com.lexicon.boundary.ProgramMilestoneBoundary
import com.lexicon.boundary.ProgramRewardBoundary

/** A program on its way to the table that keeps it. */
fun ProgramBoundary.toUserEntity(): ProgramEntity =
    ProgramEntity(
        id = id,
        level = level,
        sortOrder = order,
        titleJson = title.encodeLocalized(),
        descriptionJson = description.encodeLocalized(),
        difficulty = difficulty,
        estimatedDays = estimatedDays,
        visibility = visibility,
        configJson = configJson,
        isUserCreated = true,
    )

fun ProgramEntity.toBoundary(): ProgramBoundary =
    ProgramBoundary(
        id = id,
        level = level,
        order = sortOrder,
        title = titleJson.decodeLocalized(),
        description = descriptionJson.decodeLocalized(),
        difficulty = difficulty,
        estimatedDays = estimatedDays,
        visibility = visibility,
        configJson = configJson,
    )

fun ProgramEnrolmentEntity.toBoundary(): ProgramEnrolmentBoundary =
    ProgramEnrolmentBoundary(programId, startedAtEpochDay, status, completedAtEpochDay)

fun ProgramDayEntity.toBoundary(): ProgramDayBoundary =
    ProgramDayBoundary(programId, epochDay, activitiesJson, appliedRulesJson, isComplete)

fun ProgramMilestoneEntity.toBoundary(): ProgramMilestoneBoundary = ProgramMilestoneBoundary(programId, milestoneId, achievedAtEpochDay)

fun ProgramRewardEntity.toBoundary(): ProgramRewardBoundary = ProgramRewardBoundary(programId, rewardId, grantedAtEpochDay)
