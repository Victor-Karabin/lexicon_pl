package com.lexicon.presentation.course

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.course.Course
import com.lexicon.interactors.course.CourseId
import com.lexicon.interactors.course.LessonId
import com.lexicon.interactors.course.LessonSummary
import com.lexicon.interactors.course.completedCount
import com.lexicon.interactors.presets.LocalizedText
import com.lexicon.interactors.presets.resolve
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.koinViewModel

private val StatusIconSize = 24.dp
private const val LOCKED_ALPHA = 0.45f

@Composable
fun CourseScreen(
    onLessonSelected: (LessonId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    CourseContent(uiState = uiState, onLessonSelected = onLessonSelected, modifier = modifier)
}

@Composable
private fun CourseContent(
    uiState: CourseUiState,
    onLessonSelected: (LessonId) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState is CourseUiState.Loading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        uiState is CourseUiState.Loaded && uiState.isEmpty ->
            Box(
                modifier = modifier.fillMaxSize().padding(Dimens.spacingXl),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.course_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

        uiState is CourseUiState.Loaded ->
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Dimens.spacingLarge),
            ) {
                uiState.courses.forEach { course ->
                    item(key = course.id.value) {
                        CourseHeader(course = course, languageTag = uiState.languageTag)
                    }
                    items(course.lessons, key = { it.id.value }) { lesson ->
                        LessonRow(lesson = lesson, onClick = { onLessonSelected(lesson.id) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spacingMedium))
                    }
                }
            }
    }
}

@Composable
private fun CourseHeader(
    course: Course,
    languageTag: String,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium)) {
            Text(
                text = course.title.resolve(languageTag),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.course_progress, course.completedCount, course.lessons.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.spacingSmall),
            )
            LinearProgressIndicator(
                progress = { course.completedFraction() },
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingSmall),
            )
        }
    }
}

@Composable
private fun LessonRow(
    lesson: LessonSummary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = lesson.isUnlocked, onClick = onClick)
            .alpha(if (lesson.isUnlocked) 1f else LOCKED_ALPHA)
            .padding(Dimens.spacingMedium),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LessonStatusIcon(lesson)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.course_lesson_number, lesson.number),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${lesson.wordCount} ${pluralStringResource(R.plurals.presets_word_count_label, lesson.wordCount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LessonStatusIcon(lesson: LessonSummary) {
    val (icon, tint) = when {
        lesson.isCompleted -> Icons.Default.CheckCircle to LexiconSuccess
        !lesson.isUnlocked -> Icons.Default.Lock to MaterialTheme.colorScheme.onSurfaceVariant
        else -> Icons.Default.RadioButtonUnchecked to MaterialTheme.colorScheme.primary
    }
    Icon(
        imageVector = icon,
        contentDescription = if (lesson.isUnlocked) null else stringResource(R.string.course_lesson_locked),
        tint = tint,
        modifier = Modifier.size(StatusIconSize),
    )
}

private fun Course.completedFraction(): Float = if (lessons.isEmpty()) 0f else completedCount.toFloat() / lessons.size

private fun previewCourse(): Course =
    Course(
        id = CourseId("krok-a1"),
        order = 1,
        level = "A1",
        title = LocalizedText(mapOf("en" to "Polski krok po kroku 1")),
        lessons = listOf(
            LessonSummary(LessonId("1"), 1, "PIERWSZY DZIEŃ W SZKOLE", 8, isCompleted = true, isUnlocked = true),
            LessonSummary(LessonId("2"), 2, "CZEŚĆ, SKĄD JESTEŚ?", 10, isCompleted = true, isUnlocked = true),
            LessonSummary(LessonId("3"), 3, "MAMI, KTO TO JEST?", 6, isCompleted = false, isUnlocked = true),
            LessonSummary(LessonId("4"), 4, "JAKI JESTEŚ?", 9, isCompleted = false, isUnlocked = false),
        ).toImmutableList(),
    )

@LightDarkPreview
@Composable
private fun CoursePreview() {
    LexiconTheme {
        CourseContent(
            uiState = CourseUiState.Loaded(courses = persistentListOf(previewCourse())),
            onLessonSelected = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun CourseEmptyPreview() {
    LexiconTheme {
        CourseContent(uiState = CourseUiState.Loaded(courses = persistentListOf()), onLessonSelected = {})
    }
}
