package com.lexicon.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandableFlowRow(
    collapsedLines: Int,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = Dimens.spacingSmall,
    verticalSpacing: Dp = Dimens.spacingSmall,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
        overflow = FlowRowOverflow.expandOrCollapseIndicator(
            expandIndicator = {
                TextButton(onClick = { expanded = true }) { Text(stringResource(R.string.action_more)) }
            },
            collapseIndicator = {
                TextButton(onClick = { expanded = false }) { Text(stringResource(R.string.action_less)) }
            },
        ),
    ) { content() }
}
