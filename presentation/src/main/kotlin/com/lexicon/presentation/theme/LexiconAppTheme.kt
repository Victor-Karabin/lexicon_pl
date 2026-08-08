package com.lexicon.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.interactors.settings.ThemeMode
import com.lexicon.presentation.settings.SettingsViewModel

@Composable
fun LexiconAppTheme(
    viewModel: SettingsViewModel = hiltViewModel(),
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
