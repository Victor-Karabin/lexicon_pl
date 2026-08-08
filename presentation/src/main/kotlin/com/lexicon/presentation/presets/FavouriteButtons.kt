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
import com.lexicon.interactors.presets.PresetFavouriteState
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.LexiconError

@Composable
fun FavouriteButton(
    isFavourite: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = contentDescription,
            tint = if (isFavourite) LexiconError else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PresetFavouriteButton(
    state: PresetFavouriteState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (state) {
        PresetFavouriteState.ALL -> Icons.Default.Favorite
        PresetFavouriteState.SOME -> Icons.Default.HeartBroken
        PresetFavouriteState.NONE -> Icons.Default.FavoriteBorder
    }
    val description = stringResource(
        when (state) {
            PresetFavouriteState.ALL -> R.string.favourite_preset_remove
            else -> R.string.favourite_preset_add
        },
    )
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (state == PresetFavouriteState.NONE) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                LexiconError
            },
        )
    }
}
