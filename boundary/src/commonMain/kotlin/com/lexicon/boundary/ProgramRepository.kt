package com.lexicon.boundary

import kotlinx.coroutines.flow.Flow

interface ProgramRepository {
    suspend fun getPrograms(): List<ProgramBoundary>

    fun observePrograms(): Flow<List<ProgramBoundary>>

    suspend fun getProgram(id: String): ProgramBoundary?

    suspend fun saveProgram(program: ProgramBoundary)

    suspend fun enrolment(programId: String): ProgramEnrolmentBoundary?

    suspend fun activeEnrolment(): ProgramEnrolmentBoundary?

    fun observeActiveEnrolment(): Flow<ProgramEnrolmentBoundary?>

    suspend fun saveEnrolment(enrolment: ProgramEnrolmentBoundary)

    suspend fun day(
        programId: String,
        epochDay: Long,
    ): ProgramDayBoundary?

    suspend fun daysBetween(
        programId: String,
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<ProgramDayBoundary>

    suspend fun countCompleteDays(programId: String): Int

    suspend fun saveDay(day: ProgramDayBoundary)

    suspend fun milestones(programId: String): List<ProgramMilestoneBoundary>

    suspend fun grantMilestone(milestone: ProgramMilestoneBoundary)

    suspend fun rewards(programId: String): List<ProgramRewardBoundary>

    suspend fun grantReward(reward: ProgramRewardBoundary)
}
