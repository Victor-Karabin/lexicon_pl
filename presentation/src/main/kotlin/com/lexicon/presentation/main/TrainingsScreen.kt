package com.lexicon.presentation.main

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
import com.lexicon.presentation.theme.Dimens

@Composable
fun TrainingsScreen(
    onTrainingSelected: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(trainingCatalog) { entry ->
            ListItem(
                headlineContent = {
                    Text(
                        text = entry.displayName,
                        color =
                            if (entry.isEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                },
                supportingContent =
                    if (entry.isEnabled) {
                        null
                    } else {
                        { Text("Coming soon") }
                    },
                modifier =
                    if (entry.isEnabled) {
                        Modifier.clickable { onTrainingSelected(entry.id) }
                    } else {
                        Modifier
                    },
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun ComingSoonScreen(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(Dimens.spacingMedium)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text("Coming soon")
    }
}
