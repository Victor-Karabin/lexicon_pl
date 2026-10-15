package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.LexiconTheme

fun wordRowActionsWidth(canDelete: Boolean) = if (canDelete) DeleteActionWidth * 2 else DeleteActionWidth

private val PreviewRowHeight = 76.dp

@Composable
fun WordRowActions(
    onChangePresets: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.width(DeleteActionWidth).fillMaxHeight()) {
            ChangePresetsAction(onClick = onChangePresets)
        }
        onDelete?.let { delete ->
            Box(modifier = Modifier.width(DeleteActionWidth).fillMaxHeight()) {
                DeleteAction(onClick = delete)
            }
        }
    }
}

@Composable
private fun ChangePresetsAction(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.word_change_presets),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun WordRowActionsPreview() {
    LexiconTheme {
        Box(modifier = Modifier.width(wordRowActionsWidth(canDelete = true)).height(PreviewRowHeight)) {
            WordRowActions(onChangePresets = {}, onDelete = {})
        }
    }
}
