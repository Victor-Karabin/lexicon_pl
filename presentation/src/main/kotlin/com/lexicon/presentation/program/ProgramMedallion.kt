package com.lexicon.presentation.program

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.lexicon.presentation.theme.component.Medallion
import com.lexicon.presentation.theme.component.MedallionIcon
import com.lexicon.presentation.theme.component.MedallionSize
import com.lexicon.presentation.theme.component.TileSkin

@Composable
fun ProgramMedallion(
    skin: TileSkin,
    modifier: Modifier = Modifier,
    size: Dp = MedallionSize,
) {
    Medallion(skin = skin, modifier = modifier, size = size) {
        MedallionIcon(Icons.Default.Favorite, skin)
    }
}
