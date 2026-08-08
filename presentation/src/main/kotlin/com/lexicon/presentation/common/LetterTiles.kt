package com.lexicon.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexicon.presentation.theme.LexiconTheme

data class LetterTile(val id: Int, val char: Char)

fun shuffleIntoTiles(text: String): List<LetterTile> = text.mapIndexed { index, char -> LetterTile(index, char) }.shuffled()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterTileGrid(
    tiles: List<LetterTile>,
    onTileSelected: (LetterTile) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TileDimens.spacing),
        verticalArrangement = Arrangement.spacedBy(TileDimens.spacing),
    ) {
        tiles.forEach { tile ->
            Card(
                modifier = Modifier
                    .size(TileDimens.size)
                    .clickable { onTileSelected(tile) },
                shape = RoundedCornerShape(TileDimens.cornerRadius),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(tile.char.toString(), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

private object TileDimens {
    val size = 48.dp
    val spacing = 8.dp
    val cornerRadius = 8.dp
}

@LightDarkPreview
@Composable
private fun LetterTileGridStatesPreview() {
    LexiconTheme {
        Surface {
            Column(
                modifier = Modifier.padding(TileDimens.spacing),
                verticalArrangement = Arrangement.spacedBy(TileDimens.spacing),
            ) {
                LetterTileGrid(tiles = shuffleIntoTiles("kot"), onTileSelected = {})
                LetterTileGrid(tiles = shuffleIntoTiles("przyjaciel"), onTileSelected = {})
                LetterTileGrid(tiles = emptyList(), onTileSelected = {})
            }
        }
    }
}
