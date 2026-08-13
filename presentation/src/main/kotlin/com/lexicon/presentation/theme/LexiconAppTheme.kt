package com.lexicon.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lexicon.interactors.settings.ThemeMode
import com.lexicon.presentation.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LexiconAppTheme(
    viewModel: SettingsViewModel = koinViewModel(),
    content: @Composable () -> Unit,
) {
    val settings by viewModel.uiState.collectAsState()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    LexiconTheme(darkTheme = darkTheme, content = content)
}
