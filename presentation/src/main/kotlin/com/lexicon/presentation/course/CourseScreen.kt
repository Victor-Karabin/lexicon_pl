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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
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
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.koinViewModel

private val StatusIconSize = 24.dp
private const val LOCKED_ALPHA = 0.45f

@Composable
fun CourseScreen(
    onCourseSelected: (CourseId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    CourseContent(uiState = uiState, onCourseSelected = onCourseSelected, modifier = modifier)
}

@Composable
private fun CourseContent(
    uiState: CourseUiState,
    onCourseSelected: (CourseId) -> Unit,
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
            // One tile per course rather than every lesson of every course laid out
            // end to end: a course is twenty-odd lessons, and the tab is meant to
            // show what there is to work through, not all of it at once.
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(Dimens.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                items(uiState.courses, key = { it.id.value }) { course ->
                    CourseTile(
                        course = course,
                        languageTag = uiState.languageTag,
                        onClick = { onCourseSelected(course.id) },
                    )
                }
            }
    }
}

/**
 * A course as a card on the Plan tab, the way a preset appears in the Vocabulary
 * tab. Its lessons live one tap away, on the course's own screen.
 */
@Composable
private fun CourseTile(
    course: Course,
    languageTag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = LexiconShapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.level,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = course.title.resolve(languageTag),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.course_progress, course.completedCount, course.lessons.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.spacingTiny),
                )
                LinearProgressIndicator(
                    progress = { course.completedFraction() },
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingSmall),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A course's progress, above its lessons.
 *
 * [showTitle] is off on the course's own screen, where the top bar names it already
 * and repeating it reads as a mistake.
 */
@Composable
internal fun CourseHeader(
    course: Course,
    languageTag: String,
    showTitle: Boolean = true,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium)) {
            if (showTitle) {
                Text(
                    text = course.title.resolve(languageTag),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.course_progress, course.completedCount, course.lessons.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = if (showTitle) Dimens.spacingSmall else 0.dp),
            )
            LinearProgressIndicator(
                progress = { course.completedFraction() },
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingSmall),
            )
        }
    }
}

@Composable
internal fun LessonRow(
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

internal fun Course.completedFraction(): Float = if (lessons.isEmpty()) 0f else completedCount.toFloat() / lessons.size

internal fun previewCourse(): Course =
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
            onCourseSelected = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun CourseEmptyPreview() {
    LexiconTheme {
        CourseContent(uiState = CourseUiState.Loaded(courses = persistentListOf()), onCourseSelected = {})
    }
}
