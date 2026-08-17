package com.lexicon.interactors.settings

import kotlinx.coroutines.flow.Flow

interface ObserveSettingsUseCase {
    operator fun invoke(): Flow<AppSettings>
}
