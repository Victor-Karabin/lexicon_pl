package com.lexicon.application.program

import com.lexicon.interactors.program.AdvanceProgramDayUseCase
import com.lexicon.interactors.program.GetProgramDayUseCase
import com.lexicon.interactors.program.NextProgramTrainingUseCase
import com.lexicon.interactors.program.ProgramLaunch
import com.lexicon.interactors.program.StartProgramSessionUseCase
import com.lexicon.model.program.ProgramId
import com.lexicon.model.training.TrainingType

class NextProgramTrainingUseCaseImpl(
    private val getDay: GetProgramDayUseCase,
    private val advanceDay: AdvanceProgramDayUseCase,
    private val startSession: StartProgramSessionUseCase,
) : NextProgramTrainingUseCase {
    override suspend fun next(id: ProgramId): ProgramLaunch? = runnable(id, getDay(id) != null)

    override suspend fun advance(id: ProgramId): ProgramLaunch? = runnable(id, advanceDay(id) != null)

    private suspend fun runnable(
        id: ProgramId,
        hasDay: Boolean,
    ): ProgramLaunch? {
        if (!hasDay) return null

        val words = startSession(id)?.wordIds ?: return null
        var queued = getDay(id)?.nextTraining

        while (queued != null && !queued.training.canRunWith(words.size)) {
            if (advanceDay(id) == null) return null
            queued = getDay(id)?.nextTraining
        }

        return queued?.let { ProgramLaunch(training = it.training, wordIds = words) }
    }
}

private fun TrainingType.canRunWith(wordCount: Int): Boolean = wordCount >= minimumWords
