package com.lexicon.application.program

import com.lexicon.boundary.ProgramRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.program.GetProgramUseCase
import com.lexicon.interactors.program.ObserveActiveProgramUseCase
import com.lexicon.interactors.program.ObserveProgramsUseCase
import com.lexicon.interactors.program.Program
import com.lexicon.model.program.ProgramId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val programOrder = compareBy<Program>({ it.order }, { it.level })

class ObserveProgramsUseCaseImpl(
    private val repository: ProgramRepository,
) : ObserveProgramsUseCase {
    override fun invoke(): Flow<ImmutableList<Program>> =
        repository.observePrograms().map { programs ->
            programs.map { it.toProgram() }.sortedWith(programOrder).toImmutableList()
        }
}

class GetProgramUseCaseImpl(
    private val repository: ProgramRepository,
) : GetProgramUseCase {
    override suspend fun invoke(id: ProgramId): Program? = repository.getProgram(id.value)?.toProgram()
}

class ObserveActiveProgramUseCaseImpl(
    private val repository: ProgramRepository,
    private val vocabulary: VocabularyRepository,
) : ObserveActiveProgramUseCase {
    override fun invoke(): Flow<Program?> =
        combine(repository.observePrograms(), vocabulary.observeStudySetIds()) { programs, studySet ->
            if (studySet.isEmpty()) {
                null
            } else {
                programs
                    .map { it.toProgram() }
                    .sortedWith(programOrder)
                    .firstOrNull { it.config.dailyPlan.queue.isNotEmpty() }
            }
        }.distinctUntilChanged()
}
