package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.lexicon.model.vocabulary.ExampleSentence
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.LexiconShapes
import com.lexicon.presentation.theme.component.GradientTile
import com.lexicon.presentation.theme.component.muted
import com.lexicon.presentation.theme.component.tileSkin

private val CardImageHeight = 220.dp

@Composable
fun WordCardFace(
    text: String,
    translation: String,
    transcription: String,
    imageUrl: String?,
    example: String,
    onPronounce: () -> Unit,
    onSpeakExample: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
) {
    val skin = tileSkin(highlighted = true)

    GradientTile(skin = skin, modifier = modifier) {
        imageUrl?.let { url ->
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
                Text(
                    text = translation,
                    style = MaterialTheme.typography.titleMedium,
                    color = skin.muted(),
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.onTile,
                )
                if (transcription.isNotBlank()) {
                    Text(
                        text = "[$transcription]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = skin.muted(),
                    )
                }
            }
            IconButton(onClick = onPronounce) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.word_pronounce, text),
                    tint = skin.onTile,
                )
            }
            onEdit?.let { edit ->
                IconButton(onClick = edit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cards_edit),
                        tint = skin.onTile,
                    )
                }
            }
        }

        ExampleSentenceRow(
            example = ExampleSentence.of(example, word = text),
            onPlay = onSpeakExample,
            color = skin.muted(),
        )
    }
}
