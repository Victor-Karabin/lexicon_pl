package com.lexicon.presentation.course

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.vocabularycourse.VocabularyCourse
import com.lexicon.model.course.Course
import com.lexicon.model.course.CourseId
import com.lexicon.model.course.LessonId
import com.lexicon.model.course.LessonSummary
import com.lexicon.model.vocabulary.CefrLevel
import com.lexicon.model.vocabulary.LocalizedText
import com.lexicon.model.vocabulary.resolve
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.GradientTile
import com.lexicon.presentation.theme.component.LexiconProgressBar
import com.lexicon.presentation.theme.component.Medallion
import com.lexicon.presentation.theme.component.MedallionIcon
import com.lexicon.presentation.theme.component.MedallionText
import com.lexicon.presentation.theme.component.StatChip
import com.lexicon.presentation.theme.component.TileChips
import com.lexicon.presentation.theme.component.muted
import com.lexicon.presentation.theme.component.tileSkin
import com.lexicon.presentation.vocabularycourse.CourseMedallion
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.koinViewModel

private val StatusIconSize = 24.dp
private const val LOCKED_ALPHA = 0.45f
private const val DESCRIPTION_LINES = 2

private const val TRACK_ALPHA = 0.25f

@Composable
fun PlanScreen(
    onCourseSelected: (CourseId) -> Unit,
    onOpenCourseSettings: () -> Unit,
    onConjugationSelected: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlanViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlanContent(
        uiState = uiState,
        onCourseSelected = onCourseSelected,
        onOpenCourseSettings = onOpenCourseSettings,
        onConjugationSelected = onConjugationSelected,
        modifier = modifier,
    )
}

@Composable
private fun PlanContent(
    uiState: PlanUiState,
    onCourseSelected: (CourseId) -> Unit,
    onOpenCourseSettings: () -> Unit,
    onConjugationSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState is PlanUiState.Loading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        uiState is PlanUiState.Loaded ->

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(Dimens.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                item(key = "vocabulary-course") {
                    VocabularyCourseTile(course = uiState.course, onClick = onOpenCourseSettings)
                }

                item(key = "courses-heading") { SectionHeading(stringResource(R.string.plan_courses)) }

                items(uiState.courses.filter { it.lessons.isNotEmpty() }, key = { it.id.value }) { course ->
                    CourseTile(
                        course = course,
                        languageTag = uiState.languageTag,
                        onClick = { onCourseSelected(course.id) },
                    )
                }

                item(key = "conjugation-course") { ConjugationCourseTile(onClick = onConjugationSelected) }
            }
    }
}

@Composable
private fun ConjugationCourseTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val skin = tileSkin()

    GradientTile(skin = skin, modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
            Medallion(skin = skin) { MedallionIcon(Icons.Default.Translate, skin) }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.conjugation_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.onTile,
                )
                Text(
                    text = stringResource(R.string.training_conjugation_blurb),
                    style = MaterialTheme.typography.bodySmall,
                    color = skin.muted(),
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = skin.muted(),
            )
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Dimens.spacingSmall, bottom = Dimens.spacingTiny),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VocabularyCourseTile(
    course: VocabularyCourse,
    onClick: () -> Unit,
) {
    val skin = tileSkin(highlighted = true)

    GradientTile(skin = skin, onClick = onClick) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CourseMedallion(skin = skin)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.vocabulary_course_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.onTile,
                )
                Text(
                    text = stringResource(R.string.vocabulary_course_settings_scope),
                    style = MaterialTheme.typography.bodySmall,
                    color = skin.muted(),
                    maxLines = DESCRIPTION_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = skin.onTile,
            )
        }

        TileChips {
            StatChip(
                icon = Icons.Default.AutoStories,
                text = stringResource(R.string.vocabulary_course_new_a_day, course.settings.newWordsADay),
                skin = skin,
            )
            StatChip(
                icon = Icons.Default.Translate,
                text = stringResource(R.string.vocabulary_course_reviews_a_day, course.settings.reviewsADay),
                skin = skin,
            )
            StatChip(
                icon = Icons.Default.FitnessCenter,
                text = stringResource(R.string.vocabulary_course_trainings_count, course.totalTrainings),
                skin = skin,
            )
        }
    }
}

@Composable
private fun CourseTile(
    course: Course,
    languageTag: String,
    onClick: () -> Unit,
) {
    val skin = tileSkin()

    GradientTile(skin = skin, onClick = onClick) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Medallion(skin = skin) { MedallionText(course.level?.name.orEmpty(), skin) }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.title.resolve(languageTag),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.onTile,
                )
                Text(
                    text = stringResource(R.string.course_progress, course.completedCount, course.lessons.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = skin.muted(),
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = skin.muted(),
            )
        }

        LexiconProgressBar(
            progress = { course.completedFraction() },
            color = skin.onTile,
            trackColor = skin.onTile.copy(alpha = TRACK_ALPHA),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun CourseHeader(
    course: Course,
    languageTag: String,
    showTitle: Boolean = true,
) {
    val skin = tileSkin(highlighted = true)

    GradientTile(skin = skin, modifier = Modifier.padding(Dimens.spacingMedium)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Medallion(skin = skin) { MedallionText(course.level?.name.orEmpty(), skin) }
            Column(modifier = Modifier.weight(1f)) {
                if (showTitle) {
                    Text(
                        text = course.title.resolve(languageTag),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.onTile,
                    )
                }
                Text(
                    text = stringResource(R.string.course_progress, course.completedCount, course.lessons.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = skin.muted(),
                )
            }
        }
        LexiconProgressBar(
            progress = { course.completedFraction() },
            color = skin.onTile,
            trackColor = skin.onTile.copy(alpha = TRACK_ALPHA),
            modifier = Modifier.fillMaxWidth(),
        )
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
        level = CefrLevel.A1,
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
private fun PlanPreview() {
    LexiconTheme {
        PlanContent(
            uiState = PlanUiState.Loaded(courses = persistentListOf(previewCourse())),
            onCourseSelected = {},
            onOpenCourseSettings = {},
            onConjugationSelected = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun PlanEmptyPreview() {
    LexiconTheme {
        PlanContent(
            uiState = PlanUiState.Loaded(courses = persistentListOf()),
            onCourseSelected = {},
            onOpenCourseSettings = {},
            onConjugationSelected = {},
        )
    }
}
