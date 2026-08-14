package com.lexicon.presentation.presets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconShapes

@Composable
fun VocabularySearchField(
    query: String,
    placeholder: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        modifier = modifier,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.presets_clear_search))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = LexiconShapes.small,
    )
}

/**
 * The transcription and CEFR band under a word, or null when it has neither.
 *
 * Every shipped word has both. A word the learner added has no band and may have no
 * transcription, and empty brackets read as something having gone wrong rather than
 * as something simply not being there.
 */
private fun PresetWord.detailLine(): String? {
    val phonetic = transcription.takeIf { it.isNotBlank() }?.let { "[$it]" }
    val band = cefr?.name
    return when {
        phonetic != null && band != null -> "$phonetic  ·  $band"
        else -> phonetic ?: band
    }
}

@Composable
fun VocabularyWordRow(
    word: PresetWord,
    onFavouriteToggled: () -> Unit,
    onPronounce: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(
            start = Dimens.spacingMedium,
            end = Dimens.spacingSmall,
            top = Dimens.spacingSmall,
            bottom = Dimens.spacingSmall,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = word.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = word.translation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            word.detailLine()?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onPronounce) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = stringResource(R.string.word_pronounce, word.text),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FavouriteButton(
            isFavourite = word.isFavourite,
            contentDescription = stringResource(
                if (word.isFavourite) R.string.favourite_remove else R.string.favourite_add,
            ),
            onClick = onFavouriteToggled,
        )
    }
}
