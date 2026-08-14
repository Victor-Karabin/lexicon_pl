package com.lexicon.presentation.presets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.lexicon.presentation.common.ExpandableFlowRow
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
                PresetChips(
                    memberships = state.memberships,
                    languageTag = state.languageTag,
                    onToggle = onToggle,
                    // The sheet scrolls; the form it is shared with does not.
                    modifier = Modifier
                        .heightIn(max = SheetMaxHeight)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

/**
 * Presets as a grid of chips, a lit one meaning the word is in it.
 *
 * Shared with the new-word form, where the same question is asked of a word that
 * does not exist yet — so this knows nothing about persisting a choice, only about
 * showing one.
 *
 * [collapsedLines] keeps seventy-odd chips from burying the rest of a form; the
 * sheet, which has nothing below them, passes null and shows the lot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetChips(
    memberships: List<PresetMembership>,
    languageTag: String,
    onToggle: (PresetId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    collapsedLines: Int? = null,
) {
    ExpandableFlowRow(
        // Int.MAX_VALUE rather than a branch here: no indicator is drawn unless a
        // row is actually hidden, so "all of them" needs no special case.
        collapsedLines = collapsedLines ?: Int.MAX_VALUE,
        modifier = modifier,
        verticalSpacing = ChipRowSpacing,
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

/** Chips carry their own padding, so the rows sit closer than the usual spacing. */
private val ChipRowSpacing = 2.dp
