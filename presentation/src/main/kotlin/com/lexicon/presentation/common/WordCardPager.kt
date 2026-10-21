package com.lexicon.presentation.common

import androidx.annotation.StringRes
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.component.LexiconProgressBar

data class WordCardPage(
    val id: VocabularyId,
    val text: String,
    val translation: String,
    val transcription: String,
    val imageUrl: String?,
    val example: String,
)

@Composable
fun WordCardPager(
    @StringRes title: Int,
    @StringRes lastButtonLabel: Int,
    isLoading: Boolean,
    index: Int,
    total: Int,
    card: WordCardPage?,
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
        topBar = { TrainingTopBar(title = stringResource(title), onClose = onClose) },
        bottomBar = {
            if (card != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (index > 0) {
                        TextButton(onClick = onPrevious) { Text(stringResource(R.string.cards_back)) }
                    }
                    Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                        Text(stringResource(if (index >= total - 1) lastButtonLabel else R.string.cards_next))
                    }
                }
            }
        },
    ) { padding ->
        when {
            isLoading ->
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            card == null ->
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
                        progress = { (index + 1f) / total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${index + 1} / $total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WordCardFace(
                        text = card.text,
                        translation = card.translation,
                        transcription = card.transcription,
                        imageUrl = card.imageUrl,
                        example = card.example,
                        onPronounce = onPronounce,
                        onSpeakExample = onSpeakExample,
                        grammar = rememberWordGrammar(card.id),
                        onEdit = onEdit,
                    )
                }
        }
    }
}
