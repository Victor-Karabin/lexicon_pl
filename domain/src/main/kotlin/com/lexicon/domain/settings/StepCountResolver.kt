package com.lexicon.domain.settings

import com.lexicon.boundary.SettingsRepository
import javax.inject.Inject

class StepCountResolver
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) {
        suspend fun resolve(requestedStepCount: Int?): Int = requestedStepCount ?: settingsRepository.getSettings().stepCount
    }
