package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lexicon.presentation.theme.Dimens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BottomActionPanel(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(Dimens.spacingSmall),
    insets: WindowInsets = WindowInsets(0),
    content: @Composable RowScope.() -> Unit,
) {
    if (WindowInsets.isImeVisible) return
    Surface(tonalElevation = Dimens.spacingTiny, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(insets)
                .padding(horizontal = Dimens.spacingMedium, vertical = Dimens.spacingSmall),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
