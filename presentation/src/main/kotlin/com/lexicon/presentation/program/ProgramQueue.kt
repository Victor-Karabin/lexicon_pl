package com.lexicon.presentation.program

import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.program.AdvanceProgramDayUseCase
import com.lexicon.interactors.program.GetProgramDayUseCase
import com.lexicon.interactors.program.ProgramId
import com.lexicon.interactors.program.StartProgramSessionUseCase
import com.lexicon.presentation.common.TrainingRequirements.minimumWordsFor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ProgramLaunch(
    val training: String,
    val wordIds: ImmutableList<VocabularyId>,
)

class ProgramQueue(
    private val getDay: GetProgramDayUseCase,
    private val advanceDay: AdvanceProgramDayUseCase,
    private val startSession: StartProgramSessionUseCase,
) {
    suspend fun next(id: ProgramId): ProgramLaunch? = runnable(id, getDay(id) != null)

    suspend fun advance(id: ProgramId): ProgramLaunch? = runnable(id, advanceDay(id) != null)

    private suspend fun runnable(
        id: ProgramId,
        hasDay: Boolean,
    ): ProgramLaunch? {
        if (!hasDay) return null

        var queued = getDay(id)?.nextTraining
        var words = startSession(id)?.wordIds ?: persistentListOf()

        while (queued != null && words.size < minimumWordsFor(queued.training)) {
            if (advanceDay(id) == null) return null
            queued = getDay(id)?.nextTraining
            words = startSession(id)?.wordIds ?: persistentListOf()
        }

        return queued?.let { ProgramLaunch(training = it.training, wordIds = words) }
    }
}
