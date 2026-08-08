package com.lexicon.presentation.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.interactors.sync.CatalogSyncStatus
import com.lexicon.interactors.sync.SyncStepStatus
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkFontScalePreview
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import java.text.NumberFormat

private val BrandBadgeSize = 88.dp
private val BrandIconSize = 44.dp
private val StatusIconSize = 22.dp
private val StatusCardMaxWidth = 420.dp
private const val PENDING_ALPHA = 0.45f

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
        Brand()

        Spacer(modifier = Modifier.height(Dimens.spacingXxl))

        StatusCard(status = status, modifier = Modifier.widthIn(max = StatusCardMaxWidth))

        // Reserved rather than conditional, so the card does not jump up the screen when the
        // retry appears — the layout should not move while the user is reading it.
        AnimatedVisibility(visible = status.isBlocked, enter = fadeIn(), exit = fadeOut()) {
            Column(
                modifier = Modifier.padding(top = Dimens.spacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.sync_blocked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onRetry, modifier = Modifier.padding(top = Dimens.spacingMedium)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(StatusIconSize))
                    Text(
                        text = stringResource(R.string.sync_retry),
                        modifier = Modifier.padding(start = Dimens.spacingSmall),
                    )
                }
            }
        }
    }
}

@Composable
private fun Brand() {
    Box(
        modifier = Modifier.size(BrandBadgeSize).background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(BrandIconSize),
        )
    }
    Text(
        text = stringResource(R.string.splash_title),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = Dimens.spacingLarge),
    )
    Text(
        text = stringResource(R.string.splash_tagline),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = Dimens.spacingSmall),
    )
}

@Composable
private fun StatusCard(
    status: CatalogSyncStatus,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = LexiconShapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(Dimens.spacingMedium)) {
            SyncStepRow(stringResource(R.string.sync_step_vocabulary), status.vocabulary)
            Spacer(modifier = Modifier.height(Dimens.spacingMedium))
            SyncStepRow(stringResource(R.string.sync_step_presets), status.presets)

            // One bar for the whole job, so the wait has a visible end. Animated so a step
            // finishing reads as progress rather than a jump.
            val progress by animateFloatAsState(
                targetValue = status.completedFraction(),
                label = "sync progress",
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingMedium),
                color = if (status.isBlocked) LexiconError else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** One line per step, each carrying its own state so a slow step is not mistaken for a stuck one. */
@Composable
private fun SyncStepRow(
    label: String,
    status: SyncStepStatus,
) {
    Row(
        modifier = Modifier.fillMaxWidth().alpha(if (status is SyncStepStatus.Pending) PENDING_ALPHA else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
    ) {
        // Crossfaded because these swap while the user is looking at them, and a popping icon
        // reads as a glitch rather than a state change. Faded on the icon kind rather than the
        // status itself, so a count changing does not restart the animation.
        Crossfade(targetState = status.icon(), label = "step icon") { icon ->
            Box(modifier = Modifier.size(StatusIconSize), contentAlignment = Alignment.Center) {
                when (icon) {
                    StepIcon.PENDING ->
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                    StepIcon.RUNNING ->
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(StatusIconSize))

                    StepIcon.DONE ->
                        Icon(Icons.Default.Check, contentDescription = null, tint = LexiconSuccess)

                    StepIcon.FAILED ->
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = LexiconError)
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
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

private enum class StepIcon { PENDING, RUNNING, DONE, FAILED }

private fun SyncStepStatus.icon(): StepIcon =
    when (this) {
        is SyncStepStatus.Pending -> StepIcon.PENDING
        is SyncStepStatus.InProgress -> StepIcon.RUNNING
        is SyncStepStatus.Complete -> StepIcon.DONE
        is SyncStepStatus.Failed -> StepIcon.FAILED
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
                stringResource(R.string.sync_up_to_date, status.total.grouped())
            } else {
                stringResource(
                    R.string.sync_changed,
                    status.total.grouped(),
                    status.added.grouped(),
                    status.updated.grouped(),
                    status.removed.grouped(),
                )
            }
    }

/** Grouped by the reader's locale: "2,219" is a count, "2219" is a serial number. */
private fun Int.grouped(): String = NumberFormat.getIntegerInstance().format(this)

private fun CatalogSyncStatus.completedFraction(): Float {
    val settled = listOf(vocabulary, presets).count {
        it is SyncStepStatus.Complete || it is SyncStepStatus.Failed
    }
    return settled / 2f
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
private fun SplashStartingPreview() {
    LexiconTheme {
        SplashContent(status = CatalogSyncStatus(vocabulary = SyncStepStatus.InProgress), onRetry = {})
    }
}

@LightDarkFontScalePreview
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
