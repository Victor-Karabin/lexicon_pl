package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme

private val UnavailableIconSize = 64.dp

/**
 * Shown when a training drew no questions it could ask. The gate catches a study set
 * that is too small; this catches what is left — a draw the training could not turn
 * into steps, which used to leave the screen loading with nothing on the way.
 */
@Composable
fun TrainingUnavailableContent(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Dimens.spacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(UnavailableIconSize),
        )
        Text(
            text = stringResource(R.string.training_unavailable_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.spacingLarge),
        )
        Text(
            text = stringResource(R.string.training_unavailable_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.spacingMedium),
        )
        Button(onClick = onClose, modifier = Modifier.padding(top = Dimens.spacingXl)) {
            Text(stringResource(R.string.training_unavailable_action))
        }
    }
}

@LightDarkPreview
@Composable
private fun TrainingUnavailablePreview() {
    LexiconTheme { TrainingUnavailableContent(onClose = {}) }
}
