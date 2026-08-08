package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.lexicon.presentation.theme.LexiconTheme

/** Shared top bar for every training screen: a title plus a close action back to the home screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingTopBar(
    title: String,
    onClose: () -> Unit,
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        },
    )
}

@LightDarkPreview
@Composable
private fun TrainingTopBarStatesPreview() {
    LexiconTheme {
        Surface {
            Column {
                TrainingTopBar(title = "Dictation", onClose = {})
                // Longest title in the catalog, to check it isn't truncated.
                TrainingTopBar(title = "Pronunciation Check", onClose = {})
            }
        }
    }
}
