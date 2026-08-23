package com.lexicon.application.program

import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.ReviewScheduleRepository
import com.lexicon.common.Clock
import com.lexicon.interactors.program.DeleteProgramUseCase
import com.lexicon.interactors.program.EnrolInProgramUseCase
import com.lexicon.interactors.program.EnrolmentStatus
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.LeaveProgramUseCase
import com.lexicon.interactors.program.ObserveActiveEnrolmentUseCase
import com.lexicon.interactors.program.ObserveProgramsUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramEnrolment
import com.lexicon.interactors.program.ResetProgramUseCase
import com.lexicon.interactors.program.ResolveProgramScopeUseCase
import com.lexicon.model.program.ProgramId
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

class ResetProgramUseCaseImpl(
    private val repository: ProgramRepository,
    private val getProgram: GetProgramUseCase,
    private val resolveScope: ResolveProgramScopeUseCase,
    private val reviews: ReviewScheduleRepository,
    private val clock: Clock,
) : ResetProgramUseCase {
    override suspend fun invoke(id: ProgramId) {
        repository.clearProgress(id.value)

        getProgram(id)?.let { program ->
            reviews.forget(resolveScope(program).map { it.value })
        }

        repository.enrolment(id.value)?.let { enrolment ->
            repository.saveEnrolment(
                enrolment.copy(startedAtEpochDay = clock.todayEpochDay(), completedAtEpochDay = null),
            )
        }
    }
}

class DeleteProgramUseCaseImpl(
    private val repository: ProgramRepository,
) : DeleteProgramUseCase {
    override suspend fun invoke(id: ProgramId) = repository.deleteProgram(id.value)
}
