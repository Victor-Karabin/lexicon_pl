package com.lexicon.presentation.review

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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.Word
import com.lexicon.model.vocabulary.WordStatus
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.WordCardFace
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.koinViewModel

private val ChoiceIconSize = 20.dp

@Composable
fun ReviewWordsScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewWordsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReviewWordsContent(
        uiState = uiState,
        onClose = onClose,
        onStatusChosen = viewModel::onStatusChosen,
        onDeleted = viewModel::onDeleted,
        onPronounce = viewModel::onPronounce,
        onSpeakExample = viewModel::onSpeakExample,
        modifier = modifier,
    )
}

@Composable
private fun ReviewWordsContent(
    uiState: ReviewWordsUiState,
    onClose: () -> Unit,
    onStatusChosen: (WordStatus) -> Unit,
    onDeleted: () -> Unit,
    onPronounce: () -> Unit,
    onSpeakExample: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.review_words_title), onClose = onClose) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                uiState.isLoading -> CircularProgressIndicator()

                uiState.isFinished ->
                    Column(
                        modifier = Modifier.padding(Dimens.spacingXl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingLarge),
                    ) {
                        Text(
                            text = stringResource(R.string.review_words_done, uiState.reviewed),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = onClose) { Text(stringResource(R.string.review_words_close)) }
                    }

                else ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Dimens.spacingMedium),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
                    ) {
                        Text(
                            text = stringResource(R.string.review_words_waiting, uiState.waiting),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        uiState.current?.let { word ->
                            WordCardFace(
                                text = word.text,
                                translation = word.translation,
                                transcription = word.transcription,
                                imageUrl = null,
                                example = word.example,
                                onPronounce = onPronounce,
                                onSpeakExample = onSpeakExample,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                        ) {
                            Choice(
                                label = stringResource(R.string.review_words_to_learn),
                                icon = Icons.Default.School,
                                isChosen = uiState.chosen == WordStatus.TO_LEARN,
                                onClick = { onStatusChosen(WordStatus.TO_LEARN) },
                                modifier = Modifier.weight(1f),
                            )
                            Choice(
                                label = stringResource(R.string.review_words_favourite),
                                icon = Icons.Default.Favorite,
                                isChosen = uiState.chosen == WordStatus.FAVOURITE,
                                tint = LexiconError,
                                onClick = { onStatusChosen(WordStatus.FAVOURITE) },
                                modifier = Modifier.weight(1f),
                            )
                            Choice(
                                label = stringResource(R.string.review_words_known),
                                icon = Icons.Default.CheckCircle,
                                isChosen = uiState.chosen == WordStatus.KNOWN,
                                tint = LexiconSuccess,
                                onClick = { onStatusChosen(WordStatus.KNOWN) },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        OutlinedButton(
                            onClick = onDeleted,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(ChoiceIconSize),
                            )
                            Text(
                                text = stringResource(R.string.review_words_delete),
                                modifier = Modifier.padding(start = Dimens.spacingSmall),
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun Choice(
    label: String,
    icon: ImageVector,
    isChosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    val colors = if (isChosen) {
        ButtonDefaults.buttonColors(containerColor = tint)
    } else {
        ButtonDefaults.outlinedButtonColors(contentColor = tint)
    }

    if (isChosen) {
        Button(onClick = onClick, colors = colors, modifier = modifier) { ChoiceLabel(label, icon) }
    } else {
        OutlinedButton(onClick = onClick, colors = colors, modifier = modifier) { ChoiceLabel(label, icon) }
    }
}

@Composable
private fun ChoiceLabel(
    label: String,
    icon: ImageVector,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(ChoiceIconSize))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@LightDarkPreview
@Composable
private fun ReviewWordsPreview() {
    LexiconTheme {
        ReviewWordsContent(
            uiState = ReviewWordsUiState(
                isLoading = false,
                words = persistentListOf(
                    Word(VocabularyId(1), "kot", "cat", "kɔt", example = "Mam czarnego **kota**."),
                ),
                waiting = 42,
            ),
            onClose = {},
            onStatusChosen = {},
            onDeleted = {},
            onPronounce = {},
            onSpeakExample = {},
        )
    }
}
