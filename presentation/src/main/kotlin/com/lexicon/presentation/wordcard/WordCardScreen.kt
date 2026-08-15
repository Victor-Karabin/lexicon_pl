package com.lexicon.presentation.wordcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.lexicon.interactors.wordcard.WordCardStep
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.common.TrainingTopBar
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.GradientTile
import com.lexicon.presentation.theme.component.muted
import com.lexicon.presentation.theme.component.tileSkin
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.koinViewModel

private val CardImageHeight = 220.dp

/**
 * Word Card: a deck to read, with nothing to answer.
 *
 * It ends where it ends — there is no score to report, so it closes rather than
 * handing off to the result screen every other training finishes on.
 */
@Composable
fun WordCardScreen(
    onClose: () -> Unit,
    onEditWord: (id: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WordCardViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onClose()
    }
    // An edit lands back here, and the card should show what was just changed.
    LaunchedEffect(Unit) { viewModel.load() }

    WordCardContent(
        uiState = uiState,
        onClose = onClose,
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onPronounce = viewModel::onPronounce,
        onEdit = { uiState.current?.let { onEditWord(it.vocabularyItemId) } },
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
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TrainingTopBar(title = stringResource(R.string.word_card_title), onClose = onClose) },
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
                            stringResource(if (uiState.isLast) R.string.word_card_done else R.string.cards_next),
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
                    modifier = Modifier.fillMaxSize().padding(padding).padding(Dimens.spacingMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
                ) {
                    LinearProgressIndicator(
                        progress = { (uiState.index + 1f) / uiState.cards.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${uiState.index + 1} / ${uiState.cards.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Card(card = uiState.current!!, onPronounce = onPronounce, onEdit = onEdit)
                }
        }
    }
}

@Composable
private fun Card(
    card: WordCardStep,
    onPronounce: () -> Unit,
    onEdit: () -> Unit,
) {
    val skin = tileSkin(highlighted = true)

    GradientTile(skin = skin) {
        card.imageUrl?.let { url ->
            SubcomposeAsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(CardImageHeight).clip(LexiconShapes.small),
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = skin.onTile)
                    }
                },
                error = {},
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                // English first: the side the learner already knows is the way in.
                Text(
                    text = card.translation,
                    style = MaterialTheme.typography.titleMedium,
                    color = skin.muted(),
                )
                Text(
                    text = card.text,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.onTile,
                )
                if (card.transcription.isNotBlank()) {
                    Text(
                        text = "[${card.transcription}]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = skin.muted(),
                    )
                }
            }
            IconButton(onClick = onPronounce) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.word_pronounce, card.text),
                    tint = skin.onTile,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.cards_edit),
                    tint = skin.onTile,
                )
            }
        }
    }
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
            onEdit = {},
        )
    }
}
