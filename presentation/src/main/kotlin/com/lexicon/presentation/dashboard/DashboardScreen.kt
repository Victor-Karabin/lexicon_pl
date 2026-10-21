package com.lexicon.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.conjugation.ConjugationCourse
import com.lexicon.interactors.countries.CountryCourse
import com.lexicon.interactors.declension.DeclensionCourse
import com.lexicon.interactors.numbers.NumberCourse
import com.lexicon.interactors.vocabularycourse.VocabularyCourse
import com.lexicon.model.course.CourseId
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabularycourse.CourseProgress
import com.lexicon.model.vocabularycourse.ProgressMetric
import com.lexicon.model.vocabularycourse.ProgressMetricType
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.course.CourseTile
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.GradientTile
import com.lexicon.presentation.theme.component.Medallion
import com.lexicon.presentation.theme.component.MedallionIcon
import com.lexicon.presentation.theme.component.MedallionText
import com.lexicon.presentation.theme.component.StatChip
import com.lexicon.presentation.theme.component.TileChips
import com.lexicon.presentation.theme.component.TileSkin
import com.lexicon.presentation.theme.component.muted
import com.lexicon.presentation.theme.component.tileSkin
import com.lexicon.presentation.vocabularycourse.CourseMedallion
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
private val ButtonIconSize = 18.dp

private const val TRACK_ALPHA = 0.25f

@Composable
fun DashboardScreen(
    onStartTraining: (training: String, wordIds: List<VocabularyId>) -> Unit,
    onOpenCards: () -> Unit,
    onOpenConjugation: (String) -> Unit,
    onOpenDeclension: (String) -> Unit,
    onOpenNumbers: () -> Unit,
    onOpenCountries: () -> Unit,
    onReviewWords: () -> Unit,
    onCourseSelected: (CourseId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.launch) {
        uiState.launch?.let {
            onStartTraining(it.training.id, it.wordIds)
            viewModel.onLaunchHandled()
        }
    }
    LaunchedEffect(uiState.openCards) {
        if (uiState.openCards) {
            onOpenCards()
            viewModel.onLaunchHandled()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onResumed()
        viewModel.refreshCourses()
    }

    DashboardContent(
        uiState = uiState,
        onContinue = viewModel::onContinue,
        onOpenConjugation = onOpenConjugation,
        onOpenDeclension = onOpenDeclension,
        onOpenNumbers = onOpenNumbers,
        onOpenCountries = onOpenCountries,
        onReviewWords = onReviewWords,
        onCourseSelected = onCourseSelected,
        modifier = modifier,
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onContinue: () -> Unit,
    onOpenConjugation: (String) -> Unit,
    onOpenDeclension: (String) -> Unit,
    onOpenNumbers: () -> Unit,
    onOpenCountries: () -> Unit,
    onReviewWords: () -> Unit,
    onCourseSelected: (CourseId) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        else ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.spacingMedium),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            ) {
                uiState.conjugationCourses.forEach { course ->
                    ConjugationCourseCard(course = course, onOpen = { onOpenConjugation(course.id) })
                }

                uiState.declensionCourses.forEach { course ->
                    DeclensionCourseCard(course = course, onOpen = { onOpenDeclension(course.id) })
                }

                uiState.numberCourse?.let { NumberCourseCard(course = it, onOpen = onOpenNumbers) }

                uiState.countryCourse?.let { CountryCourseCard(course = it, onOpen = onOpenCountries) }

                CourseCard(uiState = uiState, onContinue = onContinue)

                uiState.courses.forEach { course ->
                    CourseTile(course = course, languageTag = uiState.languageTag, onClick = { onCourseSelected(course.id) })
                }

                if (uiState.wordsToReview > 0) {
                    ReviewWordsCard(waiting = uiState.wordsToReview, onClick = onReviewWords)
                }

                uiState.studyTime?.let { StudyTimeCard(history = it) }
            }
    }
}

@Composable
private fun ConjugationCourseCard(
    course: ConjugationCourse,
    onOpen: () -> Unit,
) {
    val progress = course.progress
    DrillCourseCard(
        title = stringResource(R.string.conjugation_title),
        medallion = stringResource(R.string.conjugation_medallion),
        fraction = progress.fraction,
        summary = stringResource(R.string.conjugation_progress, progress.mastered, progress.total),
        attempted = progress.attempted,
        total = progress.total,
        onOpen = onOpen,
    ) { skin ->
        StatChip(icon = Icons.Default.Translate, text = stringResource(R.string.conjugation_verbs_chip, progress.verbs), skin = skin)
    }
}

@Composable
private fun DeclensionCourseCard(
    course: DeclensionCourse,
    onOpen: () -> Unit,
) {
    val progress = course.progress
    DrillCourseCard(
        title = stringResource(R.string.declension_title),
        medallion = stringResource(R.string.declension_medallion),
        fraction = progress.fraction,
        summary = stringResource(R.string.declension_progress, progress.mastered, progress.total),
        attempted = progress.attempted,
        total = progress.total,
        onOpen = onOpen,
    ) { skin ->
        StatChip(icon = Icons.Default.Translate, text = stringResource(R.string.declension_words_chip, progress.words), skin = skin)
    }
}

@Composable
private fun NumberCourseCard(
    course: NumberCourse,
    onOpen: () -> Unit,
) {
    val progress = course.progress
    DrillCourseCard(
        title = stringResource(R.string.numbers_title),
        medallion = stringResource(R.string.numbers_medallion),
        fraction = progress.fraction,
        summary = stringResource(R.string.numbers_progress, progress.mastered, progress.total),
        attempted = progress.attempted,
        total = progress.total,
        onOpen = onOpen,
    ) { skin ->
        StatChip(icon = Icons.Default.Tag, text = stringResource(R.string.numbers_count, progress.items), skin = skin)
    }
}

@Composable
private fun CountryCourseCard(
    course: CountryCourse,
    onOpen: () -> Unit,
) {
    val progress = course.progress
    DrillCourseCard(
        title = stringResource(R.string.countries_title),
        medallion = stringResource(R.string.countries_medallion),
        fraction = progress.fraction,
        summary = stringResource(R.string.countries_progress, progress.mastered, progress.total),
        attempted = progress.attempted,
        total = progress.total,
        onOpen = onOpen,
    ) { skin ->
        StatChip(
            icon = Icons.Default.Public,
            text = pluralStringResource(R.plurals.countries_count, progress.items, progress.items),
            skin = skin,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DrillCourseCard(
    title: String,
    medallion: String,
    fraction: Float,
    summary: String,
    attempted: Int,
    total: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    chips: @Composable (TileSkin) -> Unit,
) {
    val skin = tileSkin(highlighted = true)

    GradientTile(skin = skin, modifier = modifier.fillMaxWidth(), onClick = onOpen) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(fraction = fraction, skin = skin, description = summary) {
                Medallion(skin = skin) { MedallionText(medallion, skin) }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dashboard_continuing),
                    style = MaterialTheme.typography.labelMedium,
                    color = skin.muted(),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.onTile,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = skin.muted(),
                )
            }
        }

        TileChips {
            chips(skin)
            StatChip(
                icon = Icons.Default.CheckCircle,
                text = stringResource(R.string.drill_attempted_chip, attempted, total),
                skin = skin,
            )
        }
    }
}

@Composable
private fun CourseCard(
    uiState: DashboardUiState,
    onContinue: () -> Unit,
) {
    val course = uiState.course ?: return
    val skin = tileSkin(highlighted = true)
    val round = stringResource(R.string.vocabulary_course_round, course.round + 1, course.completedInRound, course.totalTrainings)

    GradientTile(skin = skin) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(fraction = course.roundFraction, skin = skin, description = round) {
                CourseMedallion(skin = skin)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.vocabulary_course_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.onTile,
                )
                Text(
                    text = round,
                    style = MaterialTheme.typography.bodyMedium,
                    color = skin.muted(),
                )
                val counts = stringResource(R.string.vocabulary_course_learning, uiState.learningWords, uiState.knownWords)
                val words = uiState.dueWords?.let { stringResource(R.string.vocabulary_course_learning_due, counts, it) } ?: counts
                Text(
                    text = uiState.accuracy?.let { stringResource(R.string.vocabulary_course_learning_accuracy, words, it) } ?: words,
                    style = MaterialTheme.typography.bodySmall,
                    color = skin.muted(),
                )
            }
        }

        if (uiState.nothingToPractise) {
            Text(
                text = stringResource(R.string.dashboard_nothing_due),
                style = MaterialTheme.typography.bodyMedium,
                color = skin.muted(),
            )
        } else {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = skin.medallion,
                    contentColor = skin.onMedallion,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val newWords = course.newWords.size.takeIf { course.showCardsNext }
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

@Composable
private fun ReviewWordsCard(
    waiting: Int,
    onClick: () -> Unit,
) {
    val skin = tileSkin()

    GradientTile(skin = skin, onClick = onClick) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Medallion(skin = skin) { MedallionIcon(Icons.Default.Inbox, skin) }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dashboard_review_words),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.onTile,
                )
                Text(
                    text = stringResource(R.string.dashboard_review_words_waiting, waiting),
                    style = MaterialTheme.typography.bodySmall,
                    color = skin.muted(),
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(
    fraction: Float,
    skin: TileSkin,
    description: String,
    content: @Composable () -> Unit,
) {
    val track = skin.onTile.copy(alpha = TRACK_ALPHA)
    val sweep = fraction.coerceIn(0f, 1f) * FULL_TURN_DEGREES

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
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
            if (sweep > 0f) {
                drawArc(
                    color = skin.onTile,
                    startAngle = -QUARTER_TURN_DEGREES,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        content()
    }
}

@LightDarkPreview
@Composable
private fun DashboardPreview() {
    LexiconTheme {
        DashboardContent(
            uiState = DashboardUiState(
                isLoading = false,
                course = VocabularyCourse(
                    position = 3,
                    round = 1,
                    cardsSeen = true,
                    newWords = persistentListOf(VocabularyId(1), VocabularyId(2)),
                ),
                progress = CourseProgress(
                    metrics = persistentListOf(
                        ProgressMetric(ProgressMetricType.VOCABULARY, current = 128, target = 150),
                        ProgressMetric(ProgressMetricType.ACCURACY, current = 87, target = 100),
                    ),
                ),
                wordsToReview = 2400,
            ),
            onContinue = {},
            onOpenConjugation = {},
            onOpenDeclension = {},
            onOpenNumbers = {},
            onOpenCountries = {},
            onCourseSelected = {},
            onReviewWords = {},
        )
    }
}
