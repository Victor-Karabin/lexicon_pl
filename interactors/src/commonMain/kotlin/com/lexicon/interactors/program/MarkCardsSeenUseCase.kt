package com.lexicon.interactors.program

/** Records that the new-word cards have been through, so they are not shown again. */
interface MarkCardsSeenUseCase {
    suspend operator fun invoke(id: ProgramId)
}
