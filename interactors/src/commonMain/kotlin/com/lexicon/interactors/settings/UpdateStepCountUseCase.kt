package com.lexicon.interactors.settings

interface UpdateStepCountUseCase {
    suspend operator fun invoke(stepCount: Int)
}
