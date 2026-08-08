package com.lexicon.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val LightColors = lightColorScheme(primary = LexiconPrimaryLight)
private val DarkColors = darkColorScheme(primary = LexiconPrimaryDark)

@Composable
fun LexiconTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme) {
        // Paints the background itself rather than leaving it to each screen. Screens built on
        // Scaffold happened to get one; anything else — the splash, a bare Column — showed the
        // window background through, which is a fixed light colour and does not follow the theme.
        Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background, content = content)
    }
}
