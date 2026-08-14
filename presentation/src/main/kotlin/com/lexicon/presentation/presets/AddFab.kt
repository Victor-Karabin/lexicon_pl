package com.lexicon.presentation.presets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme

private const val CLOSED_ROTATION = 0f
private const val OPEN_ROTATION = 45f

/**
 * The two things a learner can add, behind one button.
 *
 * Open state is kept here rather than in a ViewModel: nothing outside this button
 * reacts to it, and it should not survive leaving the tab.
 */
@Composable
fun AddFab(
    onAddWord: () -> Unit,
    onAddPreset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOpen by remember { mutableStateOf(false) }
    // The plus becomes a close cross as the options appear, so the same button
    // visibly undoes itself.
    val rotation by animateFloatAsState(
        targetValue = if (isOpen) OPEN_ROTATION else CLOSED_ROTATION,
        label = "addFabRotation",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.vocabulary_add_word)) },
                    icon = { Icon(Icons.Default.Translate, contentDescription = null) },
                    onClick = {
                        isOpen = false
                        onAddWord()
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.vocabulary_add_preset)) },
                    icon = { Icon(Icons.Default.Category, contentDescription = null) },
                    onClick = {
                        isOpen = false
                        onAddPreset()
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            }
        }

        FloatingActionButton(onClick = { isOpen = !isOpen }) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.vocabulary_add),
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun AddFabPreview() {
    LexiconTheme {
        Surface {
            AddFab(onAddWord = {}, onAddPreset = {}, modifier = Modifier.padding(Dimens.spacingMedium))
        }
    }
}
