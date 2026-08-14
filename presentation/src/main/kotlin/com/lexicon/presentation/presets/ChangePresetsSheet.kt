package com.lexicon.presentation.presets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexicon.interactors.presets.PresetId
import com.lexicon.interactors.presets.PresetMembership
import com.lexicon.interactors.presets.resolve
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens

private val SheetMaxHeight = 420.dp

/**
 * Which presets a word belongs to, as a grid of chips: a lit chip means the word is
 * in that preset. Tapping one applies immediately — a word can be in any number of
 * presets, so there is nothing to confirm and nothing that conflicts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePresetsSheet(
    state: ChangePresetsUiState,
    onToggle: (PresetId, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingMedium)
                .padding(bottom = Dimens.spacingXl),
        ) {
            Text(
                text = stringResource(R.string.word_change_presets_title, state.word),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.word_change_presets_count,
                    state.memberCount,
                    state.memberCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.spacingSmall, bottom = Dimens.spacingMedium),
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(Dimens.spacingXl), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                PresetChips(state.memberships, state.languageTag, onToggle)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PresetChips(
    memberships: List<PresetMembership>,
    languageTag: String,
    onToggle: (PresetId, Boolean) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = SheetMaxHeight)
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        memberships.forEach { membership ->
            FilterChip(
                selected = membership.isMember,
                onClick = { onToggle(membership.preset.id, !membership.isMember) },
                label = { Text(membership.preset.title.resolve(languageTag)) },
                leadingIcon = {
                    if (membership.isMember) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                },
            )
        }
    }
}
