package com.lexicon.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.interactors.sync.CatalogSyncStatus
import com.lexicon.interactors.sync.SyncStepStatus
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme

private val StatusIconSize = 20.dp

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val status by viewModel.status.collectAsState()

    // Leaves as soon as the catalogue is ready. A blocked sync stays put: there is nothing
    // behind this screen to show without vocabulary.
    LaunchedEffect(status.isFinished, status.isBlocked) {
        if (status.isFinished && !status.isBlocked) onFinished()
    }

    SplashContent(status = status, onRetry = viewModel::start, modifier = modifier)
}

@Composable
private fun SplashContent(
    status: CatalogSyncStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Dimens.spacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Lexicon", style = MaterialTheme.typography.displaySmall)

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingXl),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            SyncStepRow(stringResource(R.string.sync_step_vocabulary), status.vocabulary)
            SyncStepRow(stringResource(R.string.sync_step_presets), status.presets)
        }

        if (status.isBlocked) {
            Text(
                text = stringResource(R.string.sync_blocked),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Dimens.spacingLarge),
            )
            Button(onClick = onRetry, modifier = Modifier.padding(top = Dimens.spacingMedium)) {
                Text(stringResource(R.string.sync_retry))
            }
        }
    }
}

/** One line per step, each carrying its own state so a slow step cannot be mistaken for a stuck one. */
@Composable
private fun SyncStepRow(
    label: String,
    status: SyncStepStatus,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
    ) {
        when (status) {
            is SyncStepStatus.Pending ->
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(StatusIconSize),
                )

            is SyncStepStatus.InProgress ->
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(StatusIconSize))

            is SyncStepStatus.Complete ->
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = LexiconSuccess,
                    modifier = Modifier.size(StatusIconSize),
                )

            is SyncStepStatus.Failed ->
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = LexiconError,
                    modifier = Modifier.size(StatusIconSize),
                )
        }

        Column {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            statusDetail(status)?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status is SyncStepStatus.Failed) {
                        LexiconError
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

/** Says what actually happened: "up to date" and "imported 2,219" are different waits. */
@Composable
private fun statusDetail(status: SyncStepStatus): String? =
    when (status) {
        is SyncStepStatus.Pending -> stringResource(R.string.sync_waiting)
        is SyncStepStatus.InProgress -> stringResource(R.string.sync_in_progress)
        is SyncStepStatus.Failed -> status.reason
        is SyncStepStatus.Complete ->
            if (status.wasAlreadyCurrent) {
                stringResource(R.string.sync_up_to_date, status.total)
            } else {
                stringResource(R.string.sync_changed, status.total, status.added, status.updated, status.removed)
            }
    }

@LightDarkPreview
@Composable
private fun SplashImportingPreview() {
    LexiconTheme {
        SplashContent(
            status = CatalogSyncStatus(
                vocabulary = SyncStepStatus.Complete(total = 2219, added = 2219, updated = 0, removed = 0),
                presets = SyncStepStatus.InProgress,
            ),
            onRetry = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun SplashUpToDatePreview() {
    LexiconTheme {
        SplashContent(
            status = CatalogSyncStatus(
                vocabulary = SyncStepStatus.Complete(total = 2219, added = 0, updated = 0, removed = 0),
                presets = SyncStepStatus.Complete(total = 72, added = 0, updated = 0, removed = 0),
            ),
            onRetry = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun SplashFailedPreview() {
    LexiconTheme {
        SplashContent(
            status = CatalogSyncStatus(
                vocabulary = SyncStepStatus.Failed("vocabulary_pl.json not found", canContinue = false),
                presets = SyncStepStatus.Failed("Skipped because the vocabulary could not be loaded", false),
            ),
            onRetry = {},
        )
    }
}
