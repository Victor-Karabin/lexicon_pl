package com.lexicon.interactors.program

import com.lexicon.model.program.ProgramId

interface MarkCardsSeenUseCase {
    suspend operator fun invoke(id: ProgramId)
}
