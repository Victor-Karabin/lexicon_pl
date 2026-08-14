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
import androidx.compose.ui.text.style.TextOverflow
import com.lexicon.presentation.theme.LexiconTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingTopBar(
    title: String,
    onClose: () -> Unit,
) {
    TopAppBar(
        // A preset's name can be longer than the bar; clip it rather than letting it
        // push the close button around.
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
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
                TrainingTopBar(title = "Pronunciation Check", onClose = {})
                // Longer than the bar: should end in an ellipsis, not wrap or
                // shove the close button aside.
                TrainingTopBar(title = "A preset whose name is far longer than the bar", onClose = {})
            }
        }
    }
}
