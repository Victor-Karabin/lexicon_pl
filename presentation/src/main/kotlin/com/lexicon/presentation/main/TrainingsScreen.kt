package com.lexicon.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.lexicon.presentation.R
import com.lexicon.presentation.common.LightDarkPreview
import com.lexicon.presentation.theme.Dimens
import com.lexicon.presentation.theme.LexiconTheme
import com.lexicon.presentation.theme.component.GradientTile
import com.lexicon.presentation.theme.component.Medallion
import com.lexicon.presentation.theme.component.MedallionIcon
import com.lexicon.presentation.theme.component.muted
import com.lexicon.presentation.theme.component.tileSkin

@Composable
fun TrainingsScreen(
    onTrainingSelected: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        items(trainingCatalog, key = { it.id }) { entry ->
            TrainingTile(entry = entry, onClick = { onTrainingSelected(entry.id) })
        }
    }
}

@Composable
private fun TrainingTile(
    entry: TrainingCatalogEntry,
    onClick: () -> Unit,
) {
    val skin = tileSkin()

    GradientTile(skin = skin, onClick = onClick.takeIf { entry.isEnabled }) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Medallion(skin = skin) { MedallionIcon(entry.icon, skin) }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = skin.onTile,
                )
                Text(
                    text = stringResource(
                        if (entry.isEnabled) entry.blurb else R.string.training_coming_soon,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = skin.muted(),
                )
            }

            if (entry.isEnabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = skin.muted(),
                )
            }
        }
    }
}

@Composable
fun ComingSoonScreen(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Dimens.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.training_coming_soon),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@LightDarkPreview
@Composable
private fun TrainingsPreview() {
    LexiconTheme {
        TrainingsScreen(onTrainingSelected = {})
    }
}
