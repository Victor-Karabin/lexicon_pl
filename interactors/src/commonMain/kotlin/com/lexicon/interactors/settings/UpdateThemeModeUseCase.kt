package com.lexicon.interactors.settings

interface UpdateThemeModeUseCase {
    suspend operator fun invoke(themeMode: ThemeMode)
}
