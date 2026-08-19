package com.lexicon.presentation.course

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lexicon.interactors.course.LessonId
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
    val uiState by viewModel.uiState.collectAsState()
    CourseDetailContent(
        uiState = uiState,
        onClose = onClose,
        onLessonSelected = onLessonSelected,
        modifier = modifier,
    )
}

@Composable
private fun CourseDetailContent(
    uiState: CourseDetailUiState,
    onClose: () -> Unit,
    onLessonSelected: (LessonId) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                            LessonRow(lesson = lesson, onClick = { onLessonSelected(lesson.id) })
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
        )
    }
}
