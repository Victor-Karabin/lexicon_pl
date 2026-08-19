package com.lexicon.presentation.presets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lexicon.interactors.presets.PresetStudySetState
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.LexiconError

@Composable
fun StudySetButton(
    isInStudySet: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = if (isInStudySet) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = contentDescription,
            tint = if (isInStudySet) LexiconError else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PresetInStudySetButton(
    state: PresetStudySetState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (state) {
        PresetStudySetState.ALL -> Icons.Default.Favorite
        PresetStudySetState.SOME -> Icons.Default.HeartBroken
        PresetStudySetState.NONE -> Icons.Default.FavoriteBorder
    }
    val description = stringResource(
        when (state) {
            PresetStudySetState.ALL -> R.string.study_set_preset_remove
            else -> R.string.study_set_preset_add
        },
    )
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (state == PresetStudySetState.NONE) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                LexiconError
            },
        )
    }
}
