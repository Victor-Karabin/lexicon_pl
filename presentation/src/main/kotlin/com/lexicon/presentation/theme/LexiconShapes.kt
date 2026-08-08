package com.lexicon.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object LexiconShapes {
    val small = RoundedCornerShape(8.dp) // chips, tiles
    val medium = RoundedCornerShape(16.dp) // cards, sheets
    val large = RoundedCornerShape(28.dp) // bubble root, FAB

    val elevationFlat = 0.dp
    val elevationRaised = 3.dp // bubble root / modal sheets only
}
