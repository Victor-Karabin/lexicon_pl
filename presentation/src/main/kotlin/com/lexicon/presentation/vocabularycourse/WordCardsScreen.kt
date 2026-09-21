package com.lexicon.presentation.vocabularycourse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexicon.interactors.vocabularycourse.WordCard
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.common.WordCardFace
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.LexiconProgressBar
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
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.cards_title), onClose = onClose) },
        bottomBar = {
            if (uiState.current != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (uiState.index > 0) {
                        TextButton(onClick = onPrevious) { Text(stringResource(R.string.cards_back)) }
                    }
                    Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(
                                if (uiState.isLast) R.string.cards_start_training else R.string.cards_next,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        when {
            uiState.isLoading ->
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            uiState.current == null ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingXl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.cards_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

            else ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(Dimens.spacingMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
                ) {
                    LexiconProgressBar(
                        progress = { (uiState.index + 1f) / uiState.cards.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${uiState.index + 1} / ${uiState.cards.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val card = uiState.current!!
                    WordCardFace(
                        text = card.text,
                        translation = card.translation,
                        transcription = card.transcription,
                        imageUrl = card.imageUrl,
                        example = card.example,
                        onPronounce = onPronounce,
                        onSpeakExample = onSpeakExample,
                        onEdit = onEdit,
                    )
                }
        }
    }
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
