package com.lexicon.presentation.course

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexicon.model.course.GrammarRule
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens

private val ChipIconSize = 18.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarSheet(
    rules: List<GrammarRule>,
    openRule: Int?,
    onRuleShown: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val rule = openRule?.let { number -> rules.firstOrNull { it.number == number } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        val dialog = LocalView.current.findViewTreeOnBackPressedDispatcherOwner()
        if (dialog != null) {
            CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides dialog) {
                BackHandler(enabled = rule != null) { onRuleShown(null) }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.spacingMedium)) {
            if (rule != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onRuleShown(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.lesson_flow_rule_back))
                    }
                    Text(
                        richText(stringResource(R.string.lesson_flow_rule_heading, rule.number, rule.title)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = Dimens.spacingXl),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                if (rule != null) {
                    RuleView(rule, showTitle = false)
                } else {
                    rules.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = richText(stringResource(R.string.lesson_flow_rule_heading, item.number, item.title)),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRuleShown(item.number) }
                                .padding(vertical = Dimens.spacingSmall),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RuleChip(
    rule: GrammarRule,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(richText(stringResource(R.string.lesson_flow_rule, rule.number, rule.title))) },
        leadingIcon = {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(ChipIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
    )
}
