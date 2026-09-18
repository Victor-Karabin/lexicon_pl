package com.lexicon.presentation.presets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lexicon.model.vocabulary.WordStatus
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess

@Composable
fun WordStatusButton(
    status: WordStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (status) {
        WordStatus.UNDEFINED -> Icons.Default.FavoriteBorder
        WordStatus.TO_LEARN -> Icons.Default.School
        WordStatus.FAVOURITE -> Icons.Default.Favorite
        WordStatus.KNOWN -> Icons.Default.CheckCircle
    }
    val tint = when (status) {
        WordStatus.UNDEFINED -> MaterialTheme.colorScheme.onSurfaceVariant
        WordStatus.TO_LEARN -> MaterialTheme.colorScheme.primary
        WordStatus.FAVOURITE -> LexiconError
        WordStatus.KNOWN -> LexiconSuccess
    }
    val description = stringResource(
        when (status) {
            WordStatus.UNDEFINED -> R.string.word_status_undefined
            WordStatus.TO_LEARN -> R.string.word_status_to_learn
            WordStatus.FAVOURITE -> R.string.word_status_favourite
            WordStatus.KNOWN -> R.string.word_status_known
        },
    )

    IconButton(onClick = onClick, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = description, tint = tint)
    }
}
