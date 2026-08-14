package com.lexicon.presentation.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.resolve
import com.lexicon.interactors.program.ActivityConfig
import com.lexicon.interactors.program.MilestoneConfig
import com.lexicon.interactors.program.Program
import com.lexicon.interactors.program.ProgramConfig
import com.lexicon.interactors.program.ProgramDifficulty
import com.lexicon.interactors.program.ProgramId
import com.lexicon.interactors.program.ProgramVisibility
import com.lexicon.interactors.program.TargetType
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconTheme
import org.koin.androidx.compose.koinViewModel

private val MilestoneDotSize = 20.dp

@Composable
fun ProgramScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgramViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    ProgramContent(
        uiState = uiState,
        onClose = onClose,
        onEnrol = viewModel::onEnrol,
        onLeave = viewModel::onLeave,
        modifier = modifier,
    )
}

@Composable
private fun ProgramContent(
    uiState: ProgramUiState,
    onClose: () -> Unit,
    onEnrol: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (uiState) {
        is ProgramUiState.Loaded -> uiState.program.title.resolve(uiState.languageTag)
        else -> stringResource(R.string.plan_programs)
    }

    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = title, onClose = onClose) },
        bottomBar = {
            if (uiState is ProgramUiState.Loaded) {
                EnrolButton(isEnrolled = uiState.isEnrolled, onEnrol = onEnrol, onLeave = onLeave)
            }
        },
    ) { padding ->
        when (uiState) {
            is ProgramUiState.Loading ->
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            is ProgramUiState.NotFound ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.program_not_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            is ProgramUiState.Loaded -> ProgramDetail(uiState, Modifier.padding(padding))
        }
    }
}

@Composable
private fun ProgramDetail(
    uiState: ProgramUiState.Loaded,
    modifier: Modifier = Modifier,
) {
    val program = uiState.program
    val config = program.config

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
    ) {
        Text(
            text = program.description.resolve(uiState.languageTag),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // What the program is for, in the terms it is measured by.
        Surface(shape = LexiconShapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium)) {
                SectionLabel(stringResource(R.string.program_goal))
                config.goals.forEach { goal ->
                    Text(
                        text = goalLine(goal.type, goal.target),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = Dimens.spacingTiny),
                    )
                }
                if (program.estimatedDays > 0) {
                    Text(
                        text = stringResource(R.string.program_duration, program.estimatedDays / DAYS_PER_WEEK),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Dimens.spacingSmall),
                    )
                }
            }
        }

        SectionLabel(stringResource(R.string.program_each_day))
        Text(
            text = stringResource(
                R.string.program_daily_summary,
                config.dailyPlan.newWords,
                config.dailyPlan.reviewWords,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        config.dailyPlan.activities.sortedBy { it.priority }.forEach { ActivityRow(it) }

        if (config.milestones.isNotEmpty()) {
            HorizontalDivider()
            SectionLabel(stringResource(R.string.program_milestones))
            config.milestones.forEach { MilestoneRow(it, uiState.languageTag) }
        }
    }
}

@Composable
private fun ActivityRow(activity: ActivityConfig) {
    Text(
        text = activityLabel(activity),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MilestoneRow(
    milestone: MilestoneConfig,
    languageTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Nothing is achieved yet — the engine that would say otherwise is not built.
        Icon(
            imageVector = Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(MilestoneDotSize),
        )
        Text(
            text = LocalizedText(milestone.title).resolve(languageTag),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        milestone.conditions.firstOrNull()?.let {
            Text(
                text = goalLine(it.type, it.target),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EnrolButton(
    isEnrolled: Boolean,
    onEnrol: () -> Unit,
    onLeave: () -> Unit,
) {
    val buttonModifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium)
    if (isEnrolled) {
        OutlinedButton(onClick = onLeave, modifier = buttonModifier) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(MilestoneDotSize))
            Text(
                text = stringResource(R.string.program_leave),
                modifier = Modifier.padding(start = Dimens.spacingSmall),
            )
        }
    } else {
        Button(onClick = onEnrol, modifier = buttonModifier) {
            Text(stringResource(R.string.program_start))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun goalLine(
    type: TargetType,
    target: Int,
): String =
    when (type) {
        TargetType.VOCABULARY -> stringResource(R.string.program_goal_words, target)
        TargetType.RETENTION -> stringResource(R.string.program_goal_retention, target)
        TargetType.STREAK -> stringResource(R.string.program_goal_streak, target)
        TargetType.TIME -> stringResource(R.string.program_goal_minutes, target)
        TargetType.EXERCISES -> stringResource(R.string.program_goal_exercises, target)
        TargetType.LESSONS -> stringResource(R.string.program_goal_lessons, target)
    }

@Composable
private fun activityLabel(activity: ActivityConfig): String =
    stringResource(
        when (activity.type) {
            com.lexicon.interactors.program.ActivityType.LEARN -> R.string.program_activity_learn
            com.lexicon.interactors.program.ActivityType.REVIEW -> R.string.program_activity_review
            com.lexicon.interactors.program.ActivityType.PRONOUNCE -> R.string.program_activity_pronounce
            com.lexicon.interactors.program.ActivityType.LISTEN -> R.string.program_activity_listen
            com.lexicon.interactors.program.ActivityType.WRITE -> R.string.program_activity_write
            com.lexicon.interactors.program.ActivityType.MIXED -> R.string.program_activity_mixed
            com.lexicon.interactors.program.ActivityType.CHALLENGE -> R.string.program_activity_challenge
        },
        activity.target,
    )

private const val DAYS_PER_WEEK = 7

private fun previewProgram(): Program =
    Program(
        id = ProgramId("a1-essentials"),
        level = "A1",
        order = 1,
        title = LocalizedText(mapOf("en" to "Polish A1")),
        description = LocalizedText(mapOf("en" to "The thousand words that carry ordinary Polish, in twelve weeks.")),
        difficulty = ProgramDifficulty.BEGINNER,
        estimatedDays = 84,
        visibility = ProgramVisibility.PUBLIC,
        config = ProgramConfig(),
    )

@LightDarkPreview
@Composable
private fun ProgramPreview() {
    LexiconTheme {
        ProgramContent(
            uiState = ProgramUiState.Loaded(program = previewProgram()),
            onClose = {},
            onEnrol = {},
            onLeave = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun ProgramNotFoundPreview() {
    LexiconTheme {
        ProgramContent(uiState = ProgramUiState.NotFound, onClose = {}, onEnrol = {}, onLeave = {})
    }
}
