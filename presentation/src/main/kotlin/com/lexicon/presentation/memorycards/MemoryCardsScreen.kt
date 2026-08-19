package com.lexicon.presentation.memorycards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.LexiconProgressBar
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryCardsScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int, tipsUsed: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoryCardsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SessionNavigationEvent.SessionComplete ->
                    onSessionComplete(event.correct, event.incorrect, event.skipped, event.tipsUsed)
            }
        }
    }

    MemoryCardsScreenContent(
        uiState = uiState,
        onClose = onClose,
        onCardSelected = viewModel::onCardSelected,
        onSkip = viewModel::onSkip,
        onNext = viewModel::onNext,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryCardsScreenContent(
    uiState: MemoryCardsUiState,
    onClose: () -> Unit,
    onCardSelected: (Int) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = "Memory Cards", onClose = onClose) },
    ) { padding ->
        when (uiState) {
            is MemoryCardsUiState.Loading ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            is MemoryCardsUiState.Loaded ->
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingMedium)) {
                    LexiconProgressBar(
                        progress = { (uiState.stepIndex + 1f) / uiState.totalSteps },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${uiState.stepIndex + 1} / ${uiState.totalSteps}",
                        modifier = Modifier.padding(top = Dimens.spacingSmall),
                        style = MaterialTheme.typography.labelMedium,
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingMedium),
                    ) {
                        items(uiState.cards, key = { it.cardId }) { card ->
                            CardTile(
                                card = card,
                                isFaceUp = uiState.isFaceUp(card),
                                isMatched = uiState.matchedItemIds.contains(card.vocabularyItemId),
                                isIncorrectFlash = uiState.incorrectFlashCardIds.contains(card.cardId),
                                enabled = uiState.isInteractive,
                                onClick = { onCardSelected(card.cardId) },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingLarge),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                    ) {
                        TextButton(onClick = onSkip, enabled = uiState.canSkip) {
                            Text("Skip")
                        }
                        if (uiState.awaitingNext) {
                            Button(onClick = onNext) {
                                Text("Next")
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun CardTile(
    card: MemoryCard,
    isFaceUp: Boolean,
    isMatched: Boolean,
    isIncorrectFlash: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val background = when {
        isMatched -> LexiconSuccess
        isIncorrectFlash -> LexiconError
        isFaceUp -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(background, RoundedCornerShape(Dimens.spacingSmall))
            .clickable(enabled = enabled && !isFaceUp, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (!isFaceUp) {
            Text("?", style = MaterialTheme.typography.headlineMedium)
        } else if (card.isImageCard && card.imageUrl != null) {
            SubcomposeAsyncImage(
                model = card.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { CircularProgressIndicator() },
                error = { Text(card.text, color = Color.White) },
            )
        } else {
            Text(card.text, color = Color.White)
        }
    }
}

private val previewCards = listOf(
    MemoryCard(cardId = 0, vocabularyItemId = 1, isImageCard = false, imageUrl = null, text = "praca"),
    MemoryCard(cardId = 1, vocabularyItemId = 2, isImageCard = false, imageUrl = null, text = "dom"),
    MemoryCard(cardId = 2, vocabularyItemId = 1, isImageCard = false, imageUrl = null, text = "work"),
    MemoryCard(cardId = 3, vocabularyItemId = 2, isImageCard = false, imageUrl = null, text = "house"),
    MemoryCard(cardId = 4, vocabularyItemId = 3, isImageCard = false, imageUrl = null, text = "kot"),
    MemoryCard(cardId = 5, vocabularyItemId = 4, isImageCard = false, imageUrl = null, text = "pies"),
    MemoryCard(cardId = 6, vocabularyItemId = 3, isImageCard = false, imageUrl = null, text = "cat"),
    MemoryCard(cardId = 7, vocabularyItemId = 4, isImageCard = false, imageUrl = null, text = "dog"),
)

@LightDarkPreview
@Composable
private fun MemoryCardsScreenPreview() {
    LexiconTheme {
        MemoryCardsScreenContent(
            uiState =
                MemoryCardsUiState.Loaded(
                    stepIndex = 1,
                    totalSteps = 5,
                    cards = previewCards,
                    flippedCardIds = listOf(2),
                    matchedItemIds = setOf(1),
                ),
            onClose = {},
            onCardSelected = {},
            onSkip = {},
            onNext = {},
        )
    }
}

@LightDarkPreview
@Composable
private fun MemoryCardsScreenInProgressPreview() {
    LexiconTheme {
        MemoryCardsScreenContent(
            uiState =
                MemoryCardsUiState.Loaded(
                    stepIndex = 2,
                    totalSteps = 10,
                    cards = previewCards,
                    matchedItemIds = setOf(1),
                    incorrectFlashCardIds = setOf(2, 3),
                    incorrectAttempts = 1,
                ),
            onClose = {},
            onCardSelected = {},
            onSkip = {},
            onNext = {},
        )
    }
}
