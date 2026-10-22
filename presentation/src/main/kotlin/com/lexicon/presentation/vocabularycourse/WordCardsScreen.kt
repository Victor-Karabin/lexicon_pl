package com.lexicon.presentation.vocabularycourse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.vocabularycourse.WordCard
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.WordCardPage
import com.lexicon.presentation.common.WordCardPager
import com.lexicon.presentation.theme.LexiconTheme
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.koinViewModel

@Composable
fun WordCardsScreen(
    onClose: () -> Unit,
    onStartTraining: (training: String, wordIds: List<VocabularyId>) -> Unit,
    onFinished: () -> Unit,
    onEditWord: (VocabularyId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordCardsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isFinished) {
        if (!uiState.isFinished) return@LaunchedEffect

        uiState.launch
            ?.let { onStartTraining(it.training.id, it.wordIds) }
            ?: onFinished()
    }

    LaunchedEffect(Unit) { viewModel.load() }

    WordCardsContent(
        uiState = uiState,
        onClose = onClose,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onPronounce = viewModel::onPronounce,
        onSpeakExample = viewModel::onSpeakExample,
        onEdit = { uiState.current?.let { card -> onEditWord(card.id) } },
        modifier = modifier,
    )
}

@Composable
private fun WordCardsContent(
    uiState: WordCardsUiState,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onPronounce: () -> Unit,
    onSpeakExample: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WordCardPager(
        title = R.string.cards_title,
        lastButtonLabel = R.string.cards_start_training,
        isLoading = uiState.isLoading,
        index = uiState.index,
        total = uiState.cards.size,
        card = uiState.current?.let {
            WordCardPage(
                id = it.id,
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
private fun WordCardsPreview() {
    LexiconTheme {
        WordCardsContent(
            uiState = WordCardsUiState(
                isLoading = false,
                cards = persistentListOf(
                    WordCard(VocabularyId(1), "woda", "water", "ˈvɔda", imageUrl = null),
                    WordCard(VocabularyId(2), "chleb", "bread", "xlɛp", imageUrl = null),
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
