package com.lexicon.presentation.presets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.lexicon.model.course.LessonPhrase
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
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

internal fun Word.detailLine(): String? {
    val phonetic = transcription.takeIf { it.isNotBlank() }?.let { "[$it]" }
    val band = cefr?.name
    return when {
        phonetic != null && band != null -> "$phonetic  ·  $band"
        else -> phonetic ?: band
    }
}

internal fun LessonPhrase.detailLine(): String? = transcription.takeIf { it.isNotBlank() }?.let { "[$it]" }

@Composable
fun VocabularyWordRow(
    word: Word,
    status: WordStatus,
    onStatusCycled: () -> Unit,
    onPronounce: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelecting: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    WordRow(
        text = word.text,
        translation = word.translation,
        detail = word.detailLine(),
        onPronounce = onPronounce,
        status = status,
        onStatusCycled = onStatusCycled,
        modifier = modifier,
        onClick = onClick,
        isSelecting = isSelecting,
        isSelected = isSelected,
        onLongClick = onLongClick,
    )
}

@Composable
fun WordRow(
    word: Word?,
    text: String,
    translation: String,
    status: WordStatus?,
    onStatusCycled: () -> Unit,
    onPronounce: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = word?.detailLine(),
) {
    WordRow(
        text = text,
        translation = translation,
        detail = detail,
        onPronounce = onPronounce,
        status = status,
        onStatusCycled = onStatusCycled,
        modifier = modifier,
    )
}

@Composable
fun ChecklistRow(
    text: String,
    isChecked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WordRow(
        text = text,
        translation = null,
        detail = null,
        onPronounce = null,
        status = null,
        modifier = modifier.toggleable(value = isChecked, role = Role.Checkbox, onValueChange = { onToggle() }),
        isSelecting = true,
        isSelected = isChecked,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WordRow(
    text: String,
    translation: String?,
    detail: String?,
    onPronounce: (() -> Unit)?,
    status: WordStatus?,
    modifier: Modifier = Modifier,
    onStatusCycled: () -> Unit = {},
    onClick: (() -> Unit)? = null,
    isSelecting: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick) else Modifier)
            .padding(
                start = if (isSelecting) Dimens.spacingSmall else Dimens.spacingMedium,
                end = Dimens.spacingSmall,
                top = Dimens.spacingSmall,
                bottom = Dimens.spacingSmall,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelecting) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                modifier = Modifier.padding(end = Dimens.spacingSmall),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            translation?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        onPronounce?.let {
            IconButton(onClick = it) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.word_pronounce, text),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (status != null) {
            WordStatusButton(status = status, onClick = onStatusCycled)
        } else if (onPronounce != null) {
            Spacer(modifier = Modifier.minimumInteractiveComponentSize())
        }
    }
}
