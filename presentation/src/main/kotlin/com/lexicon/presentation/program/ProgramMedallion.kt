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

/**
 * The badge a program leads with, wherever it is shown.
 *
 * A heart, because a program is over the words the learner starred — the same mark
 * the Vocabulary tab puts on them. In the medallion's own ink rather than a red of
 * its own: every other medallion in the app is drawn that way, and one badge shouting
 * in a different colour reads as a warning rather than as a heart. One composable
 * rather than two, so the Plan tab and the Dashboard cannot drift apart.
 */
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
