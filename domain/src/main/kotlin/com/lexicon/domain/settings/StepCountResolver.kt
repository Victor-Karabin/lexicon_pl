package com.lexicon.domain.settings

import com.lexicon.boundary.SettingsRepository

class StepCountResolver(
    private val settingsRepository: SettingsRepository,
) {
    suspend fun resolve(requestedStepCount: Int?): Int = requestedStepCount ?: settingsRepository.getSettings().stepCount
}
