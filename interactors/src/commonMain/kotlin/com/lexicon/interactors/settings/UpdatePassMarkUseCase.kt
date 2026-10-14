package com.lexicon.interactors.settings

interface UpdatePassMarkUseCase {
    suspend operator fun invoke(passMark: Int)
}
