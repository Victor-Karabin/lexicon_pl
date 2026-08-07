package com.lexicon.domain.settings

import com.lexicon.boundary.SettingsRepository
import javax.inject.Inject

/**
 * Resolves how many steps a session should have: an explicit request value wins, otherwise the
 * count configured in Settings. Read once when the session starts, so changing the setting never
 * affects a session already in progress.
 */
class StepCountResolver
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) {
        suspend fun resolve(requestedStepCount: Int?): Int = requestedStepCount ?: settingsRepository.getSettings().stepCount
    }
