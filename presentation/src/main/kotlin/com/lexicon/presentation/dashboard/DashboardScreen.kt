package com.lexicon.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.resolve
import com.lexicon.interactors.program.ProgramProgress
import com.lexicon.interactors.program.ProgressMetric
import com.lexicon.interactors.program.ProgressMetricType
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconTheme
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.koinViewModel

private const val PERCENT = 100

@Composable
fun DashboardScreen(
    onStartTraining: (training: String, wordIds: List<VocabularyId>) -> Unit,
    onGoToPlan: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.launch) {
        uiState.launch?.let {
            onStartTraining(it.training, it.wordIds)
            viewModel.onLaunchHandled()
        }
    }
    // Coming back from a session is when every figure here has just moved.
    LaunchedEffect(Unit) { viewModel.onResumed() }

    DashboardContent(
        uiState = uiState,
        onStartTraining = viewModel::onStartTraining,
        onGoToPlan = onGoToPlan,
        modifier = modifier,
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onStartTraining: () -> Unit,
    onGoToPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        uiState.program == null ->
            Column(
                modifier = modifier.fillMaxSize().padding(Dimens.spacingXl),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingLarge, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_no_program),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                // Saying where programs are is less use than going there.
                Button(onClick = onGoToPlan) {
                    Text(stringResource(R.string.dashboard_go_to_plan))
                }
            }

        else ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            ) {
                ActiveProgramCard(uiState = uiState, onStartTraining = onStartTraining)
            }
    }
}

@Composable
private fun ActiveProgramCard(
    uiState: DashboardUiState,
    onStartTraining: () -> Unit,
) {
    val program = uiState.program ?: return
    val progress = uiState.progress

    Surface(shape = LexiconShapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            Text(
                text = stringResource(R.string.dashboard_continuing),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = program.title.resolve(uiState.languageTag),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            if (progress != null) {
                Text(
                    text = stringResource(R.string.dashboard_overall, (progress.overall * PERCENT).toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { progress.overall.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                // The breakdown, because one percentage says nothing about which
                // part of the work is lagging.
                progress.metrics.forEach { MetricRow(it) }
            }

            if (uiState.streakDays > 0) {
                Text(
                    text = stringResource(R.string.dashboard_streak, uiState.streakDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (uiState.nothingDueToday) {
                Text(
                    text = stringResource(R.string.dashboard_nothing_due),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(onClick = onStartTraining, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dashboard_continue))
            }
        }
    }
}

@Composable
private fun MetricRow(metric: ProgressMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        Text(
            text = stringResource(metric.type.label()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${metric.current} / ${metric.target}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun ProgressMetricType.label(): Int =
    when (this) {
        ProgressMetricType.VOCABULARY -> R.string.dashboard_metric_vocabulary
        ProgressMetricType.MILESTONES -> R.string.dashboard_metric_milestones
        ProgressMetricType.RETENTION -> R.string.dashboard_metric_retention
        ProgressMetricType.CONSISTENCY -> R.string.dashboard_metric_consistency
        ProgressMetricType.STUDY_TIME -> R.string.dashboard_metric_study_time
        ProgressMetricType.ACCURACY -> R.string.dashboard_metric_accuracy
    }

@LightDarkPreview
@Composable
private fun DashboardNoProgramPreview() {
    LexiconTheme {
        DashboardContent(uiState = DashboardUiState(isLoading = false), onStartTraining = {}, onGoToPlan = {})
    }
}

@LightDarkPreview
@Composable
private fun DashboardActivePreview() {
    LexiconTheme {
        DashboardContent(
            uiState = DashboardUiState(
                isLoading = false,
                program = com.lexicon.interactors.program.Program(
                    id = com.lexicon.interactors.program.ProgramId("a1-essentials"),
                    level = "A1",
                    order = 1,
                    title = LocalizedText(mapOf("en" to "Polish A1")),
                    description = LocalizedText(emptyMap()),
                    difficulty = com.lexicon.interactors.program.ProgramDifficulty.BEGINNER,
                    estimatedDays = 84,
                    visibility = com.lexicon.interactors.program.ProgramVisibility.PUBLIC,
                    config = com.lexicon.interactors.program.ProgramConfig(),
                ),
                progress = ProgramProgress(
                    programId = com.lexicon.interactors.program.ProgramId("a1-essentials"),
                    metrics = persistentListOf(
                        ProgressMetric(ProgressMetricType.VOCABULARY, current = 128, target = 1000, weight = 40),
                        ProgressMetric(ProgressMetricType.MILESTONES, current = 1, target = 5, weight = 20),
                        ProgressMetric(ProgressMetricType.RETENTION, current = 87, target = 90, weight = 20),
                        ProgressMetric(ProgressMetricType.CONSISTENCY, current = 9, target = 12, weight = 20),
                    ),
                ),
                streakDays = 9,
            ),
            onStartTraining = {},
            onGoToPlan = {},
        )
    }
}
