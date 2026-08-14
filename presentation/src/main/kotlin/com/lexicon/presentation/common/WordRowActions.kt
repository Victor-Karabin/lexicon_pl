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

/** Change-presets sits alongside Delete, so a word row reveals twice the width. */
val WordRowActionsWidth = DeleteActionWidth * 2

private val PreviewRowHeight = 76.dp

/**
 * The actions behind a word row: change which presets it belongs to, or delete it.
 *
 * Delete stays rightmost — it is the destructive one, and it was there on its own
 * before this, so its position is the one people already have in their fingers.
 */
@Composable
fun WordRowActions(
    onChangePresets: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.width(DeleteActionWidth).fillMaxHeight()) {
            ChangePresetsAction(onClick = onChangePresets)
        }
        Box(modifier = Modifier.width(DeleteActionWidth).fillMaxHeight()) {
            DeleteAction(onClick = onDelete)
        }
    }
}

@Composable
private fun ChangePresetsAction(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.secondaryContainer,
        // Square edges: this one abuts the delete action rather than ending the row.
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
        Box(modifier = Modifier.width(WordRowActionsWidth).height(PreviewRowHeight)) {
            WordRowActions(onChangePresets = {}, onDelete = {})
        }
    }
}
