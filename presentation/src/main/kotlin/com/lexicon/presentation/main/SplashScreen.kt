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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.sync.CatalogSyncStatus
import com.lexicon.interactors.sync.SyncStepStatus
import com.lexicon.interactors.sync.isBlocked
import com.lexicon.interactors.sync.isFinished
import com.lexicon.interactors.sync.steps
import com.lexicon.interactors.sync.wasAlreadyCurrent
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkFontScalePreview
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import org.koin.androidx.compose.koinViewModel
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
    viewModel: SplashViewModel = koinViewModel(),
) {
    val status by viewModel.status.collectAsState()

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
            painter = painterResource(R.drawable.ic_brand_mark),
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
        Column(
            modifier = Modifier.padding(Dimens.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        ) {
            SyncStepRow(stringResource(R.string.sync_step_vocabulary), status.vocabulary)
            SyncStepRow(stringResource(R.string.sync_step_presets), status.presets)
            SyncStepRow(stringResource(R.string.sync_step_course), status.course)
            SyncStepRow(stringResource(R.string.sync_step_verbs), status.verbs)

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

private fun Int.grouped(): String = NumberFormat.getIntegerInstance().format(this)

private fun CatalogSyncStatus.completedFraction(): Float {
    val settled = steps.count { it is SyncStepStatus.Complete || it is SyncStepStatus.Failed }
    return settled.toFloat() / steps.size
}

@LightDarkPreview
@Composable
private fun SplashImportingPreview() {
    LexiconTheme {
        SplashContent(
            status = CatalogSyncStatus(
                vocabulary = SyncStepStatus.Complete(total = 2477, added = 2477, updated = 0, removed = 0),
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
                vocabulary = SyncStepStatus.Complete(total = 2477, added = 0, updated = 0, removed = 0),
                presets = SyncStepStatus.Complete(total = 73, added = 0, updated = 0, removed = 0),
                course = SyncStepStatus.Complete(total = 26, added = 0, updated = 0, removed = 0),
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
                course = SyncStepStatus.Failed("Skipped because the vocabulary could not be loaded", false),
            ),
            onRetry = {},
        )
    }
}
