package com.lexicon.presentation.wordcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.wordcard.WordCardStep
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.WordCardPage
import com.lexicon.presentation.common.WordCardPager
import com.lexicon.presentation.theme.LexiconTheme
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.koinViewModel

@Composable
fun WordCardScreen(
    onClose: () -> Unit,
    onFinished: () -> Unit,
    onEditWord: (id: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordCardViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onFinished()
    }

    var editedWordId by rememberSaveable { mutableStateOf<Long?>(null) }
    LifecycleResumeEffect(Unit) {
        editedWordId?.let { id ->
            viewModel.onWordEdited(id)
            editedWordId = null
        }
        onPauseOrDispose {}
    }

    WordCardContent(
        uiState = uiState,
        onClose = onClose,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onPronounce = viewModel::onPronounce,
        onSpeakExample = viewModel::onSpeakExample,
        onEdit = {
            uiState.current?.let {
                editedWordId = it.vocabularyItemId
                onEditWord(it.vocabularyItemId)
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun WordCardContent(
    uiState: WordCardUiState,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onPronounce: () -> Unit,
    onSpeakExample: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WordCardPager(
        title = R.string.word_card_title,
        lastButtonLabel = R.string.word_card_done,
        isLoading = uiState.isLoading,
        index = uiState.index,
        total = uiState.cards.size,
        card = uiState.current?.let {
            WordCardPage(
                id = VocabularyId(it.vocabularyItemId),
                text = it.text,
                translation = it.translation,
                transcription = it.transcription,
                imageUrl = it.imageUrl,
                example = it.example,
            )
        },
        onClose = onClose,
        onNext = onNext,
        onPrevious = onPrevious,
        onPronounce = onPronounce,
        onSpeakExample = onSpeakExample,
        onEdit = onEdit,
        modifier = modifier,
    )
}

@LightDarkPreview
@Composable
private fun WordCardPreview() {
    LexiconTheme {
        WordCardContent(
            uiState = WordCardUiState(
                isLoading = false,
                cards = persistentListOf(
                    WordCardStep(0, 1, "woda", "water", "ˈvɔda", imageUrl = null),
                    WordCardStep(1, 2, "chleb", "bread", "xlɛp", imageUrl = null),
                ),
            ),
            onClose = {},
            onNext = {},
            onPrevious = {},
            onPronounce = {},
            onSpeakExample = {},
            onEdit = {},
        )
    }
}
