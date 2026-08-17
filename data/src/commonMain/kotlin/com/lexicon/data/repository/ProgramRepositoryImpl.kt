package com.lexicon.data.repository

import com.lexicon.boundary.ProgramBoundary
import com.lexicon.boundary.ProgramDayBoundary
import com.lexicon.boundary.ProgramEnrolmentBoundary
import com.lexicon.boundary.ProgramMilestoneBoundary
import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.ProgramRewardBoundary
import com.lexicon.data.local.ProgramDao
import com.lexicon.data.local.ProgramDayEntity
import com.lexicon.data.local.ProgramEnrolmentEntity
import com.lexicon.data.local.ProgramMilestoneEntity
import com.lexicon.data.local.ProgramRewardEntity
import com.lexicon.data.local.toBoundary
import com.lexicon.data.local.toUserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val ACTIVE = "ACTIVE"

class ProgramRepositoryImpl(
    private val programDao: ProgramDao,
) : ProgramRepository {
    override suspend fun getPrograms(): List<ProgramBoundary> = programDao.getPrograms().map { it.toBoundary() }

    override fun observePrograms(): Flow<List<ProgramBoundary>> =
        programDao.observePrograms().map { programs -> programs.map { it.toBoundary() } }

    override suspend fun getProgram(id: String): ProgramBoundary? = programDao.getProgram(id)?.toBoundary()

    override suspend fun saveProgram(program: ProgramBoundary) {
        programDao.insertPrograms(listOf(program.toUserEntity()))
    }

    override suspend fun enrolment(programId: String): ProgramEnrolmentBoundary? = programDao.enrolment(programId)?.toBoundary()

    override suspend fun activeEnrolment(): ProgramEnrolmentBoundary? = programDao.enrolmentWithStatus(ACTIVE)?.toBoundary()

    override fun observeActiveEnrolment(): Flow<ProgramEnrolmentBoundary?> =
        programDao.observeEnrolmentWithStatus(ACTIVE).map { it?.toBoundary() }

    override suspend fun saveEnrolment(enrolment: ProgramEnrolmentBoundary) {
        programDao.upsertEnrolment(
            ProgramEnrolmentEntity(
                programId = enrolment.programId,
                startedAtEpochDay = enrolment.startedAtEpochDay,
                status = enrolment.status,
                completedAtEpochDay = enrolment.completedAtEpochDay,
            ),
        )
    }

    override suspend fun day(
        programId: String,
        epochDay: Long,
    ): ProgramDayBoundary? = programDao.day(programId, epochDay)?.toBoundary()

    override suspend fun daysBetween(
        programId: String,
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<ProgramDayBoundary> = programDao.daysBetween(programId, fromEpochDay, toEpochDay).map { it.toBoundary() }

    override suspend fun countCompleteDays(programId: String): Int = programDao.countCompleteDays(programId)

    override suspend fun saveDay(day: ProgramDayBoundary) {
        programDao.upsertDay(
            ProgramDayEntity(
                programId = day.programId,
                epochDay = day.epochDay,
                activitiesJson = day.activitiesJson,
                appliedRulesJson = day.appliedRulesJson,
                isComplete = day.isComplete,
            ),
        )
    }

    override suspend fun milestones(programId: String): List<ProgramMilestoneBoundary> =
        programDao.milestones(programId).map { it.toBoundary() }

    override suspend fun grantMilestone(milestone: ProgramMilestoneBoundary) {
        programDao.grantMilestone(
            ProgramMilestoneEntity(milestone.programId, milestone.milestoneId, milestone.achievedAtEpochDay),
        )
    }

    override suspend fun rewards(programId: String): List<ProgramRewardBoundary> = programDao.rewards(programId).map { it.toBoundary() }

    override suspend fun grantReward(reward: ProgramRewardBoundary) {
        programDao.grantReward(ProgramRewardEntity(reward.programId, reward.rewardId, reward.grantedAtEpochDay))
    }
}
