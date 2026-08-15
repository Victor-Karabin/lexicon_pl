package com.lexicon.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.interactors.presets.resolve
import com.lexicon.interactors.program.ProgramProgress
import com.lexicon.interactors.program.ProgressMetric
import com.lexicon.interactors.program.ProgressMetricType
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.program.ProgramMedallion
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.GradientTile
import com.lexicon.presentation.theme.component.TileSkin
import com.lexicon.presentation.theme.component.muted
import com.lexicon.presentation.theme.component.tileSkin
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.koinViewModel

private const val PERCENT = 100
private const val FULL_TURN_DEGREES = 360f
private const val QUARTER_TURN_DEGREES = 90f

private val RingSize = 72.dp
private val RingStroke = 5.dp
private val PipSize = 10.dp
private val PipBorder = 2.dp
private val MetricBarHeight = 3.dp
private val StreakIconSize = 20.dp
private val ButtonIconSize = 18.dp

/** A progress track has to stay visible against the tile it is drawn on. */
private const val TRACK_ALPHA = 0.25f

@Composable
fun DashboardScreen(
    onStartTraining: (training: String, wordIds: List<VocabularyId>) -> Unit,
    onOpenCards: (programId: String) -> Unit,
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
    LaunchedEffect(uiState.openCards) {
        if (uiState.openCards) {
            uiState.program?.let { onOpenCards(it.id.value) }
            viewModel.onLaunchHandled()
        }
    }
    // Coming back from a session is when every figure here has just moved.
    LaunchedEffect(Unit) { viewModel.onResumed() }

    DashboardContent(
        uiState = uiState,
        onContinue = viewModel::onContinue,
        onGoToPlan = onGoToPlan,
        modifier = modifier,
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onContinue: () -> Unit,
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
                ActiveProgramCard(uiState = uiState, onContinue = onContinue)
            }
    }
}

/**
 * The program the learner is on, wearing the same coat as its tile on the Plan tab —
 * the one they tapped to get here, so it should be recognisably the same thing.
 */
@Composable
private fun ActiveProgramCard(
    uiState: DashboardUiState,
    onContinue: () -> Unit,
) {
    val program = uiState.program ?: return
    val progress = uiState.progress
    val skin = tileSkin(highlighted = true)

    GradientTile(skin = skin) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The overall figure drawn around the level rather than written under it:
            // it is the one number that is about the whole program, so it belongs on
            // the thing that names the program.
            ProgressRing(fraction = progress?.overall?.toFloat() ?: 0f, skin = skin) {
                ProgramMedallion(skin = skin)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dashboard_continuing),
                    style = MaterialTheme.typography.labelMedium,
                    color = skin.muted(),
                )
                Text(
                    text = program.title.resolve(uiState.languageTag),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.onTile,
                )
                if (progress != null) {
                    Text(
                        text = stringResource(R.string.dashboard_overall, (progress.overall * PERCENT).toInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = skin.muted(),
                    )
                }
            }
            if (uiState.streakDays > 0) {
                StreakBadge(days = uiState.streakDays, skin = skin)
            }
        }

        // The breakdown, because one percentage says nothing about which part of the
        // work is lagging. Each gets its own line so a lagging one is visible as a
        // short bar rather than as arithmetic the reader has to do.
        //
        // Days studied is left out: the streak badge above is already a count of days
        // on this card, and two of them side by side read as the same figure twice.
        progress?.metrics
            ?.filterNot { it.type == ProgressMetricType.CONSISTENCY }
            ?.forEach { MetricRow(it, skin) }

        if (uiState.trainingsTotal > 0) {
            TodaysQueue(
                done = uiState.trainingsDone,
                total = uiState.trainingsTotal,
                skin = skin,
            )
        }

        when {
            uiState.isDayComplete ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = skin.onTile,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_day_done),
                        style = MaterialTheme.typography.bodyMedium,
                        color = skin.muted(),
                    )
                }

            else ->
                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = skin.medallion,
                        contentColor = skin.onMedallion,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val newWords = uiState.day?.takeIf { it.showCardsNext }?.newWords?.size
                    Icon(
                        imageVector = if (newWords != null) Icons.Default.AutoStories else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonIconSize),
                    )
                    Text(
                        text = if (newWords != null) {
                            stringResource(R.string.dashboard_meet_words, newWords)
                        } else {
                            stringResource(R.string.dashboard_continue)
                        },
                        modifier = Modifier.padding(start = Dimens.spacingSmall),
                    )
                }
        }
    }
}

/** How far through the program, as a ring drawn around whatever sits inside it. */
@Composable
private fun ProgressRing(
    fraction: Float,
    skin: TileSkin,
    content: @Composable () -> Unit,
) {
    val track = skin.onTile.copy(alpha = TRACK_ALPHA)
    val sweep = fraction.coerceIn(0f, 1f) * FULL_TURN_DEGREES

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(RingSize)) {
            val stroke = Stroke(width = RingStroke.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = FULL_TURN_DEGREES,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = skin.onTile,
                // From the top, the way a dial is read.
                startAngle = -QUARTER_TURN_DEGREES,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
        }
        content()
    }
}

/**
 * The streak, as a number worth keeping rather than a line of text.
 *
 * A run of days is the one figure a learner loses by not turning up, so it earns its
 * own corner of the card.
 */
@Composable
private fun StreakBadge(
    days: Int,
    skin: TileSkin,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = skin.onTile,
            modifier = Modifier.size(StreakIconSize),
        )
        Text(
            text = "$days",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = skin.onTile,
        )
        Text(
            text = stringResource(R.string.dashboard_streak_unit),
            style = MaterialTheme.typography.labelSmall,
            color = skin.muted(),
        )
    }
}

/**
 * Today's queue as one pip per turn at a training.
 *
 * "0 / 16" is a fact; sixteen pips are a thing to fill in. The next one up is drawn
 * ringed rather than empty, so where the learner is in the day is visible without
 * counting.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodaysQueue(
    done: Int,
    total: Int,
    skin: TileSkin,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            Text(
                text = stringResource(R.string.dashboard_trainings),
                style = MaterialTheme.typography.bodySmall,
                color = skin.muted(),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$done / $total",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = skin.onTile,
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingTiny)) {
            repeat(total) { index ->
                val pip = Modifier.size(PipSize).clip(CircleShape)
                Box(
                    modifier = when {
                        index < done -> pip.background(skin.onTile)
                        index == done -> pip.border(PipBorder, skin.onTile, CircleShape)
                        else -> pip.background(skin.onTile.copy(alpha = TRACK_ALPHA))
                    },
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    metric: ProgressMetric,
    skin: TileSkin,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            Text(
                text = stringResource(metric.type.label()),
                style = MaterialTheme.typography.bodySmall,
                color = skin.muted(),
                modifier = Modifier.weight(1f),
            )
            Text(
                // A share of answers reads as a percentage; a count reads as a count.
                text = if (metric.type == ProgressMetricType.ACCURACY) {
                    "${metric.current}%"
                } else {
                    "${metric.current} / ${metric.target}"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = skin.onTile,
            )
        }
        LinearProgressIndicator(
            progress = { if (metric.target > 0) metric.current.toFloat() / metric.target else 0f },
            color = skin.onTile,
            trackColor = skin.onTile.copy(alpha = TRACK_ALPHA),
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier.fillMaxWidth().height(MetricBarHeight),
        )
    }
}

private fun ProgressMetricType.label(): Int =
    when (this) {
        ProgressMetricType.VOCABULARY -> R.string.dashboard_metric_vocabulary
        ProgressMetricType.MILESTONES -> R.string.dashboard_metric_milestones
        ProgressMetricType.CONSISTENCY -> R.string.dashboard_metric_consistency
        ProgressMetricType.STUDY_TIME -> R.string.dashboard_metric_study_time
        ProgressMetricType.ACCURACY -> R.string.dashboard_metric_accuracy
    }

@LightDarkPreview
@Composable
private fun DashboardNoProgramPreview() {
    LexiconTheme {
        DashboardContent(uiState = DashboardUiState(isLoading = false), onContinue = {}, onGoToPlan = {})
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
                        ProgressMetric(ProgressMetricType.ACCURACY, current = 87, target = 100, weight = 20),
                        ProgressMetric(ProgressMetricType.CONSISTENCY, current = 9, target = 12, weight = 20),
                    ),
                ),
                streakDays = 9,
            ),
            onContinue = {},
            onGoToPlan = {},
        )
    }
}
