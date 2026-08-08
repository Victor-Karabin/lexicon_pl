package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.interactors.training.TrainingReadiness
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme

private val EmptyStateIconSize = 64.dp

@Composable
fun TrainingGate(
    minimumWords: Int,
    trainingName: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrainingGateViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val readiness by viewModel.readiness.collectAsState()

    LaunchedEffect(minimumWords) { viewModel.check(minimumWords) }

    when (val state = readiness) {
        null ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        is TrainingReadiness.NotEnoughWords ->
            NotEnoughWordsContent(
                trainingName = trainingName,
                required = state.required,
                available = state.available,
                onClose = onClose,
                modifier = modifier,
            )

        TrainingReadiness.Ready -> content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotEnoughWordsContent(
    trainingName: String,
    required: Int,
    available: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = trainingName, onClose = onClose) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(EmptyStateIconSize),
            )
            Text(
                text = stringResource(R.string.training_not_enough_words_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Dimens.spacingLarge),
            )
            Text(
                text = stringResource(R.string.training_not_enough_words_body, trainingName, required, available),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Dimens.spacingMedium),
            )
            Text(
                text = stringResource(R.string.training_not_enough_words_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Dimens.spacingSmall),
            )
            Button(onClick = onClose, modifier = Modifier.padding(top = Dimens.spacingXl)) {
                Text(stringResource(R.string.training_not_enough_words_action))
            }
        }
    }
}

@LightDarkPreview
@Composable
private fun NotEnoughWordsPreview() {
    LexiconTheme {
        NotEnoughWordsContent(trainingName = "Image Test", required = 6, available = 2, onClose = {})
    }
}

@LightDarkPreview
@Composable
private fun NotEnoughWordsEmptyStudySetPreview() {
    LexiconTheme {
        NotEnoughWordsContent(trainingName = "Crossword", required = 8, available = 0, onClose = {})
    }
}
