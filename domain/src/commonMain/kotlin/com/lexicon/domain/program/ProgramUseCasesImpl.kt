package com.lexicon.domain.program

import com.lexicon.boundary.ProgramRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.program.EnrolInProgramUseCase
import com.lexicon.interactors.program.EnrolmentStatus
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.LeaveProgramUseCase
import com.lexicon.interactors.program.ObserveActiveEnrolmentUseCase
import com.lexicon.interactors.program.ObserveProgramsUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramEnrolment
import com.lexicon.interactors.program.ProgramId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveProgramsUseCaseImpl(
    private val repository: ProgramRepository,
) : ObserveProgramsUseCase {
    override fun invoke(): Flow<ImmutableList<Program>> =
        repository.observePrograms().map { programs ->
            programs.map { it.toProgram() }.sortedWith(compareBy({ it.order }, { it.level })).toImmutableList()
        }
}

class GetProgramUseCaseImpl(
    private val repository: ProgramRepository,
) : GetProgramUseCase {
    override suspend fun invoke(id: ProgramId): Program? = repository.getProgram(id.value)?.toProgram()
}

class ObserveActiveEnrolmentUseCaseImpl(
    private val repository: ProgramRepository,
) : ObserveActiveEnrolmentUseCase {
    override fun invoke(): Flow<ProgramEnrolment?> = repository.observeActiveEnrolment().map { it?.toEnrolment() }
}

class EnrolInProgramUseCaseImpl(
    private val repository: ProgramRepository,
    private val clock: Clock,
) : EnrolInProgramUseCase {
    override suspend fun invoke(id: ProgramId): ProgramEnrolment {
        repository.activeEnrolment()?.let { active ->
            if (active.programId != id.value) {
                repository.saveEnrolment(active.copy(status = EnrolmentStatus.ABANDONED.name))
            }
        }

        val existing = repository.enrolment(id.value)
        val enrolment = ProgramEnrolment(
            programId = id,
            startedAtEpochDay = existing?.startedAtEpochDay ?: clock.todayEpochDay(),
            status = EnrolmentStatus.ACTIVE,
        )
        repository.saveEnrolment(enrolment.toBoundary())
        return enrolment
    }
}

class LeaveProgramUseCaseImpl(
    private val repository: ProgramRepository,
) : LeaveProgramUseCase {
    override suspend fun invoke(id: ProgramId) {
        val existing = repository.enrolment(id.value) ?: return
        repository.saveEnrolment(existing.copy(status = EnrolmentStatus.ABANDONED.name))
    }
}
