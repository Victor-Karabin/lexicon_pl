package com.lexicon.presentation.course

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.model.course.LessonId
import com.lexicon.model.vocabulary.resolve
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun CourseDetailScreen(
    onClose: () -> Unit,
    onLessonSelected: (LessonId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CourseDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CourseDetailContent(
        uiState = uiState,
        onClose = onClose,
        onLessonSelected = onLessonSelected,
        onLessonReset = viewModel::onLessonReset,
        modifier = modifier,
    )
}

@Composable
private fun CourseDetailContent(
    uiState: CourseDetailUiState,
    onClose: () -> Unit,
    onLessonSelected: (LessonId) -> Unit,
    onLessonReset: (LessonId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var resetting by rememberSaveable { mutableStateOf<String?>(null) }
    val loaded = uiState as? CourseDetailUiState.Loaded
    loaded?.course?.lessons?.firstOrNull { it.id.value == resetting }?.let { lesson ->
        AlertDialog(
            onDismissRequest = { resetting = null },
            title = { Text(stringResource(R.string.course_reset_lesson_title, lesson.number)) },
            text = { Text(stringResource(R.string.course_reset_lesson_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onLessonReset(lesson.id)
                    resetting = null
                }) { Text(stringResource(R.string.course_reset_lesson_confirm)) }
            },
            dismissButton = { TextButton(onClick = { resetting = null }) { Text(stringResource(R.string.course_reset_lesson_cancel)) } },
        )
    }
    val title = when (uiState) {
        is CourseDetailUiState.Loaded -> uiState.course.title.resolve(uiState.languageTag)
        else -> stringResource(R.string.course_title)
    }

    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = title, onClose = onClose) },
    ) { padding ->
        when (uiState) {
            is CourseDetailUiState.Loading ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

            is CourseDetailUiState.NotFound ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.course_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            is CourseDetailUiState.Loaded ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    CourseHeader(
                        course = uiState.course,
                        languageTag = uiState.languageTag,
                        showTitle = false,
                    )
                    LazyColumn(contentPadding = PaddingValues(bottom = Dimens.spacingLarge)) {
                        items(uiState.course.lessons, key = { it.id.value }) { lesson ->
                            LessonRow(
                                lesson = lesson,
                                onClick = { onLessonSelected(lesson.id) },
                                onReset = { resetting = lesson.id.value },
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spacingMedium))
                        }
                    }
                }
        }
    }
}

@LightDarkPreview
@Composable
private fun CourseDetailPreview() {
    LexiconTheme {
        CourseDetailContent(
            uiState = CourseDetailUiState.Loaded(course = previewCourse()),
            onClose = {},
            onLessonSelected = {},
            onLessonReset = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun CourseDetailNotFoundPreview() {
    LexiconTheme {
        CourseDetailContent(
            uiState = CourseDetailUiState.NotFound,
            onClose = {},
            onLessonSelected = {},
            onLessonReset = {},
        )
    }
}
