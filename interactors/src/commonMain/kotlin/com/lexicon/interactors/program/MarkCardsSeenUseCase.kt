package com.lexicon.interactors.program

interface MarkCardsSeenUseCase {
    suspend operator fun invoke(id: ProgramId)
}
