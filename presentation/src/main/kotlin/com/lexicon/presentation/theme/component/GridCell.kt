package com.lexicon.presentation.theme.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val CORNER_FRACTION = 0.2f

private const val LETTER_SIZE_RATIO = 0.5f

@Composable
fun GridCell(
    size: Dp,
    background: Color,
    border: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val corner = RoundedCornerShape(size * CORNER_FRACTION)
    Box(
        modifier = modifier
            .size(size)
            .padding(1.dp)
            .background(background, corner)
            .border(1.dp, border, corner),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun gridLetterStyle(
    size: Dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
): TextStyle =
    MaterialTheme.typography.titleMedium.copy(
        color = color,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        fontSize = (size.value * LETTER_SIZE_RATIO).sp,
        lineHeight = (size.value * LETTER_SIZE_RATIO).sp,
    )
