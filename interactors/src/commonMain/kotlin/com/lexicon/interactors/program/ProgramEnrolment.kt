package com.lexicon.interactors.program

import com.lexicon.model.program.ProgramId

data class ProgramEnrolment(
    val programId: ProgramId,
    val startedAtEpochDay: Long,
    val status: EnrolmentStatus,
    val completedAtEpochDay: Long? = null,
)

enum class EnrolmentStatus { ACTIVE, COMPLETED, ABANDONED }
