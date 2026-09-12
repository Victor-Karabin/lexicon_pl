package com.lexicon.presentation.dashboard

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.program.DailyStudyTime
import com.lexicon.interactors.program.StudyTimeHistory
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.GradientTile
import com.lexicon.presentation.theme.component.Medallion
import com.lexicon.presentation.theme.component.MedallionIcon
import com.lexicon.presentation.theme.component.TileSkin
import com.lexicon.presentation.theme.component.muted
import com.lexicon.presentation.theme.component.tileSkin
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val MINUTES_PER_HOUR = 60

private const val BASELINE_ALPHA = 0.25f
private const val QUIET_DAY_ALPHA = 0.18f

private val PlotHeight = 72.dp
private val BarGap = 4.dp
private val BarCorner = 4.dp
private val BaselineThickness = 1.dp
private val QuietDayHeight = 3.dp

@Composable
internal fun StudyTimeCard(
    history: StudyTimeHistory,
    modifier: Modifier = Modifier,
) {
    val skin = tileSkin()

    GradientTile(skin = skin, modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Medallion(skin = skin) { MedallionIcon(Icons.Default.Schedule, skin) }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dashboard_study_time_week),
                    style = MaterialTheme.typography.labelMedium,
                    color = skin.muted(),
                )
                if (history.isEmpty) {
                    Text(
                        text = stringResource(R.string.dashboard_study_time_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = skin.muted(),
                    )
                } else {
                    Text(
                        text = durationLabel(history.totalMinutes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.onTile,
                    )
                    history.busiest?.let { best ->
                        Text(
                            text = stringResource(
                                R.string.dashboard_study_time_best,
                                best.weekday(),
                                durationLabel(best.studiedMinutes),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = skin.muted(),
                        )
                    }
                }
            }
        }

        if (!history.isEmpty) {
            StudyTimeChart(
                history = history,
                skin = skin,
                bars = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun StudyTimeChart(
    history: StudyTimeHistory,
    skin: TileSkin,
    bars: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val today = history.days.lastOrNull()?.epochDay
    val spoken = history.days.joinToString { day ->
        context.getString(
            R.string.dashboard_study_time_day,
            day.weekday(),
            if (day.wasStudied) {
                context.durationText(day.studiedMinutes)
            } else {
                context.getString(R.string.dashboard_study_time_nothing)
            },
        )
    }
    val description = stringResource(R.string.dashboard_study_time_chart, spoken)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlotHeight)
                .clipToBounds(),
        ) {
            val gap = BarGap.toPx()
            val corner = CornerRadius(BarCorner.toPx())
            val quiet = QuietDayHeight.toPx()
            val baseline = size.height - BaselineThickness.toPx()
            val barWidth = ((size.width - gap * (history.days.size - 1)) / history.days.size).coerceAtLeast(1f)

            drawRect(
                color = skin.onTile.copy(alpha = BASELINE_ALPHA),
                topLeft = Offset(0f, baseline),
                size = Size(size.width, BaselineThickness.toPx()),
            )

            history.days.forEachIndexed { index, day ->
                val tall = if (day.wasStudied) {
                    (history.shareOfBusiest(day) * baseline).coerceAtLeast(quiet)
                } else {
                    quiet
                }
                drawRoundRect(
                    color = if (day.wasStudied) bars else skin.onTile.copy(alpha = QUIET_DAY_ALPHA),
                    topLeft = Offset(index * (barWidth + gap), baseline - tall),
                    size = Size(barWidth, tall + corner.y),
                    cornerRadius = corner,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BarGap),
        ) {
            history.days.forEach { day ->
                val isToday = day.epochDay == today
                Text(
                    text = day.weekday(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isToday) skin.onTile else skin.muted(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun durationLabel(minutes: Int): String = LocalContext.current.durationText(minutes)

private fun Context.durationText(minutes: Int): String =
    if (minutes >= MINUTES_PER_HOUR) {
        getString(R.string.duration_hours_minutes, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
    } else {
        getString(R.string.duration_minutes, minutes)
    }

private fun DailyStudyTime.weekday(): String {
    val weekday = LocalDate.ofEpochDay(epochDay).dayOfWeek
    return weekday.getDisplayName(TextStyle.SHORT, Locale.getDefault())
}

@LightDarkPreview
@Composable
private fun StudyTimeCardPreview() {
    LexiconTheme {
        StudyTimeCard(
            history = StudyTimeHistory(
                days = listOf(720L, 0L, 1_500L, 300L, 0L, 2_400L, 900L)
                    .mapIndexed { index, seconds -> DailyStudyTime(epochDay = 20_000L - 6 + index, studiedSeconds = seconds) }
                    .toImmutableList(),
            ),
        )
    }
}

@LightDarkPreview
@Composable
private fun StudyTimeCardEmptyPreview() {
    LexiconTheme {
        StudyTimeCard(
            history = StudyTimeHistory(
                days = (0 until 7)
                    .map { DailyStudyTime(epochDay = 20_000L - 6 + it, studiedSeconds = 0L) }
                    .toImmutableList(),
            ),
        )
    }
}
