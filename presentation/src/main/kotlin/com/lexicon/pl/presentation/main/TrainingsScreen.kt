package com.lexicon.pl.presentation.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lexicon.pl.presentation.theme.Dimens

/** Names match "Software Development Specification" §9. Only Dictation is implemented so far. */
private val trainingTypes = listOf(
    "Dictation" to true,
    "Dictation Puzzle" to false,
    "Puzzle" to false,
    "Image Test" to false,
    "Word Match" to false,
    "True or False" to false,
    "Pronunciation Check" to false,
    "Memory Cards" to false,
    "Crossword" to false,
    "Word Builder" to false,
    "Mix" to false,
    "Custom Builder" to false,
)

@Composable
fun TrainingsScreen(onDictationSelected: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(trainingTypes) { (name, enabled) ->
            ListItem(
                headlineContent = {
                    Text(
                        text = name,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                supportingContent = if (enabled) null else {
                    { Text("Coming soon") }
                },
                modifier = if (enabled) {
                    Modifier.clickable(onClick = onDictationSelected)
                } else {
                    Modifier
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun ComingSoonScreen(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(Dimens.spacingMedium)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text("Coming soon")
    }
}
