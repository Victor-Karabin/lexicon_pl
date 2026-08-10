package com.lexicon.presentation.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexicon.interactors.course.CourseId
import com.lexicon.interactors.course.Lesson
import com.lexicon.interactors.course.LessonAudio
import com.lexicon.interactors.course.LessonAudioSource
import com.lexicon.interactors.course.LessonId
import com.lexicon.interactors.course.LessonSection
import com.lexicon.interactors.course.label
import com.lexicon.interactors.presets.PresetWord
import com.lexicon.interactors.presets.VocabularyId
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.presets.VocabularyWordRow
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
fun LessonScreen(
    onClose: () -> Unit,
    onTrainLesson: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LessonViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LessonContent(
        uiState = uiState,
        onClose = onClose,
        onTrainLesson = onTrainLesson,
        onCompletedToggled = viewModel::onCompletedToggled,
        onWordFavouriteToggled = viewModel::onWordFavouriteToggled,
        onPronounceWord = viewModel::onPronounceWord,
        onPlayAudio = viewModel::onPlayAudio,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonContent(
    uiState: LessonUiState,
    onClose: () -> Unit,
    onTrainLesson: (List<Long>) -> Unit,
    onCompletedToggled: (Boolean) -> Unit,
    onWordFavouriteToggled: (VocabularyId, Boolean) -> Unit,
    onPronounceWord: (PresetWord) -> Unit,
    onPlayAudio: (LessonAudio) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (uiState) {
        is LessonUiState.Loaded -> stringResource(R.string.course_lesson_number, uiState.lesson.number)
        else -> stringResource(R.string.course_title)
    }

    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = title, onClose = onClose) },
    ) { padding ->
        when (uiState) {
            is LessonUiState.Loading ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

            is LessonUiState.NotFound ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.lesson_not_found),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

            is LessonUiState.Loaded ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = Dimens.spacingXl),
                ) {
                    lessonHeader(uiState.lesson, onTrainLesson, onCompletedToggled)
                    sectionsBlock(uiState.lesson.sections)
                    wordsBlock(uiState, onWordFavouriteToggled, onPronounceWord)
                    audioBlock(uiState, onPlayAudio)
                }
        }
    }
}

private fun LazyListScope.lessonHeader(
    lesson: Lesson,
    onTrainLesson: (List<Long>) -> Unit,
    onCompletedToggled: (Boolean) -> Unit,
) = item {
    Column(modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium)) {
        Text(
            text = lesson.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            Button(
                onClick = { onTrainLesson(lesson.vocabularyIds.map { it.value }) },
                enabled = lesson.vocabularyIds.isNotEmpty(),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text(
                    text = stringResource(R.string.lesson_train),
                    modifier = Modifier.padding(start = Dimens.spacingSmall),
                )
            }
            OutlinedButton(onClick = { onCompletedToggled(!lesson.isCompleted) }) {
                Icon(
                    imageVector = if (lesson.isCompleted) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.RadioButtonUnchecked
                    },
                    contentDescription = null,
                    tint = if (lesson.isCompleted) LexiconSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        if (lesson.isCompleted) R.string.lesson_mark_incomplete else R.string.lesson_mark_complete,
                    ),
                    modifier = Modifier.padding(start = Dimens.spacingSmall),
                )
            }
        }
    }
}

private fun LazyListScope.sectionsBlock(sections: List<LessonSection>) {
    if (sections.isEmpty()) return
    item { SectionHeading(stringResource(R.string.lesson_sections)) }
    items(sections, key = { it.letter }) { section ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = Dimens.spacingMedium,
                vertical = Dimens.spacingSmall,
            ),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        ) {
            Text(
                text = section.letter,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = section.title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun LazyListScope.wordsBlock(
    uiState: LessonUiState.Loaded,
    onWordFavouriteToggled: (VocabularyId, Boolean) -> Unit,
    onPronounceWord: (PresetWord) -> Unit,
) {
    item { SectionHeading(stringResource(R.string.lesson_words)) }
    if (uiState.isLoadingWords) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(Dimens.spacingXl),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }
    itemsIndexed(uiState.words, key = { _, word -> word.id.value }) { index, word ->
        VocabularyWordRow(
            word = word,
            onFavouriteToggled = { onWordFavouriteToggled(word.id, !word.isFavourite) },
            onPronounce = { onPronounceWord(word) },
        )
        if (index < uiState.words.lastIndex) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.spacingMedium))
        }
    }
}

private fun LazyListScope.audioBlock(
    uiState: LessonUiState.Loaded,
    onPlayAudio: (LessonAudio) -> Unit,
) {
    val tracks = uiState.lesson.audio
    if (tracks.isEmpty()) return
    item { SectionHeading(stringResource(R.string.lesson_audio)) }
    tracks.groupBy { it.source }.forEach { (source, group) ->
        // The key has to survive saved state, which rules out the enum itself.
        item(key = source.name) {
            AudioGroup(source = source, tracks = group, uiState = uiState, onPlayAudio = onPlayAudio)
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(
            start = Dimens.spacingMedium,
            end = Dimens.spacingMedium,
            top = Dimens.spacingLarge,
            bottom = Dimens.spacingSmall,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioGroup(
    source: LessonAudioSource,
    tracks: List<LessonAudio>,
    uiState: LessonUiState.Loaded,
    onPlayAudio: (LessonAudio) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.spacingMedium)) {
        if (source == LessonAudioSource.WORKBOOK) {
            Text(
                text = stringResource(R.string.lesson_audio_workbook),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Dimens.spacingSmall),
            )
        }
        // The workbook recordings are not in the shared folder, so a group can be
        // entirely unplayable; saying so beats a row of dead chips.
        if (tracks.none { uiState.isPlayable(it) }) {
            Text(
                text = stringResource(R.string.lesson_audio_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Dimens.spacingSmall),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            tracks.forEach { track ->
                AudioChip(
                    track = track,
                    isEnabled = uiState.isPlayable(track),
                    isDownloading = uiState.downloadingAudio == track.file,
                    isPlaying = uiState.playingAudio == track.file,
                    onClick = { onPlayAudio(track) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioChip(
    track: LessonAudio,
    isEnabled: Boolean,
    isDownloading: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        enabled = isEnabled && !isDownloading,
        label = { Text(text = track.label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
            when {
                isDownloading ->
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )

                isPlaying ->
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = stringResource(R.string.lesson_audio_pause, track.label),
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )

                else ->
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.lesson_audio_play, track.label),
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
            }
        },
    )
}

private val previewLesson = Lesson(
    id = LessonId("krok-a1-01"),
    courseId = CourseId("krok-a1"),
    number = 1,
    title = "PIERWSZY DZIEŃ W SZKOLE",
    sections = persistentListOf(
        LessonSection("A", "SEKRETARIAT - PREZENTACJA"),
        LessonSection("B", "ALFABET"),
    ),
    vocabularyIds = listOf(1L, 2L, 3L).map(::VocabularyId).toImmutableList(),
    audio = persistentListOf(
        LessonAudio("a1_coursebook_101a1.mp3", LessonAudioSource.COURSEBOOK, "A", 1, null, "drive-id"),
        LessonAudio("a1_coursebook_101b2.mp3", LessonAudioSource.COURSEBOOK, "B", 2, null, "drive-id"),
        LessonAudio("a1_workbook_01_L01_cwiczenie2.mp3", LessonAudioSource.WORKBOOK, null, 2, null, null),
    ),
    isCompleted = false,
)

@LightDarkPreview
@Composable
private fun LessonPreview() {
    LexiconTheme {
        LessonContent(
            uiState = LessonUiState.Loaded(
                lesson = previewLesson,
                words = persistentListOf(
                    PresetWord(VocabularyId(1), "proszę", "please", "ˈprɔʂɛ", isFavourite = true),
                    PresetWord(VocabularyId(2), "dziękuję", "thank you", "d͡ʑɛŋˈkujɛ"),
                    PresetWord(VocabularyId(3), "przepraszam", "sorry", "pʂɛˈpraʂam"),
                ),
                isLoadingWords = false,
            ),
            onClose = {},
            onTrainLesson = {},
            onCompletedToggled = {},
            onWordFavouriteToggled = { _, _ -> },
            onPronounceWord = {},
            onPlayAudio = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun LessonNotFoundPreview() {
    LexiconTheme {
        LessonContent(
            uiState = LessonUiState.NotFound,
            onClose = {},
            onTrainLesson = {},
            onCompletedToggled = {},
            onWordFavouriteToggled = { _, _ -> },
            onPronounceWord = {},
            onPlayAudio = {},
        )
    }
}
