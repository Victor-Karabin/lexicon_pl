package com.lexicon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs ORDER BY sortOrder, level")
    suspend fun getPrograms(): List<ProgramEntity>

    @Query("SELECT * FROM programs ORDER BY sortOrder, level")
    fun observePrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE id = :id")
    suspend fun getProgram(id: String): ProgramEntity?

    @Query("SELECT COUNT(*) FROM programs")
    suspend fun countPrograms(): Int

    // ---- enrolment

    @Query("SELECT * FROM program_enrolment WHERE programId = :programId")
    suspend fun enrolment(programId: String): ProgramEnrolmentEntity?

    @Query("SELECT * FROM program_enrolment WHERE status = :status LIMIT 1")
    suspend fun enrolmentWithStatus(status: String): ProgramEnrolmentEntity?

    @Query("SELECT * FROM program_enrolment WHERE status = :status LIMIT 1")
    fun observeEnrolmentWithStatus(status: String): Flow<ProgramEnrolmentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEnrolment(enrolment: ProgramEnrolmentEntity)

    // ---- days

    @Query("SELECT * FROM program_day WHERE programId = :programId AND epochDay = :epochDay")
    suspend fun day(
        programId: String,
        epochDay: Long,
    ): ProgramDayEntity?

    @Query("SELECT * FROM program_day WHERE programId = :programId AND epochDay BETWEEN :from AND :to")
    suspend fun daysBetween(
        programId: String,
        from: Long,
        to: Long,
    ): List<ProgramDayEntity>

    @Query("SELECT COUNT(*) FROM program_day WHERE programId = :programId AND isComplete = 1")
    suspend fun countCompleteDays(programId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDay(day: ProgramDayEntity)

    // ---- milestones and rewards

    @Query("SELECT * FROM program_milestone WHERE programId = :programId")
    suspend fun milestones(programId: String): List<ProgramMilestoneEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun grantMilestone(milestone: ProgramMilestoneEntity)

    @Query("SELECT * FROM program_reward WHERE programId = :programId")
    suspend fun rewards(programId: String): List<ProgramRewardEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun grantReward(reward: ProgramRewardEntity)

    // ---- seeding

    /**
     * Replaces the shipped catalogue, keeping the learner's own programs.
     *
     * Enrolments, days, milestones and rewards are keyed by program id and are never
     * touched here, so a re-seed cannot lose them — the same reasoning that keeps
     * preset_word_overrides alive across a catalogue rewrite.
     */
    @Transaction
    suspend fun replaceCatalog(programs: List<ProgramEntity>) {
        val mine = getUserPrograms()
        clearShippedPrograms()
        insertPrograms(programs)
        if (mine.isNotEmpty()) insertPrograms(mine)
    }

    @Query("SELECT * FROM programs WHERE isUserCreated = 1")
    suspend fun getUserPrograms(): List<ProgramEntity>

    @Query("DELETE FROM programs WHERE isUserCreated = 0")
    suspend fun clearShippedPrograms()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<ProgramEntity>)
}
