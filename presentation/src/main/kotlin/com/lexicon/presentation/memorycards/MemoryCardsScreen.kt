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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.lexicon.presentation.common.SessionNavigationEvent
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconError
import com.lexicon.presentation.theme.LexiconSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryCardsScreen(
    onSessionComplete: (correct: Int, incorrect: Int, skipped: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoryCardsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SessionNavigationEvent.SessionComplete ->
                    onSessionComplete(event.correct, event.incorrect, event.skipped)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Memory Cards") }) },
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingMedium)) {
            LinearProgressIndicator(
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
                        onClick = { viewModel.onCardSelected(card.cardId) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingLarge),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                TextButton(onClick = viewModel::onSkip, enabled = uiState.canSkip) {
                    Text("Skip")
                }
                if (uiState.awaitingNext) {
                    Button(onClick = viewModel::onNext) {
                        Text("Next")
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
    val background =
        when {
            isMatched -> LexiconSuccess
            isIncorrectFlash -> LexiconError
            isFaceUp -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    Box(
        modifier =
            Modifier
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
