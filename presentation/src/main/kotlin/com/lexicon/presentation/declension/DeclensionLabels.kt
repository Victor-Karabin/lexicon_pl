package com.lexicon.presentation.declension

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lexicon.interactors.declension.DeclensionColumn
import com.lexicon.model.vocabulary.Gender
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.presentation.R
import com.lexicon.presentation.common.labelId

internal fun DeclensionColumn.labelId(): Int =
    when (this) {
        DeclensionColumn.SINGULAR -> R.string.grammar_singular
        DeclensionColumn.PLURAL -> R.string.grammar_plural
        DeclensionColumn.MASCULINE -> R.string.grammar_masculine
        DeclensionColumn.FEMININE -> R.string.grammar_feminine
        DeclensionColumn.NEUTER -> R.string.grammar_neuter
        DeclensionColumn.PLURAL_PERSONAL -> R.string.grammar_plural_personal
        DeclensionColumn.PLURAL_OTHER -> R.string.grammar_plural_other
    }

@Composable
internal fun wordKindLabel(
    partOfSpeech: PartOfSpeech,
    gender: Gender?,
): String {
    val name = stringResource(partOfSpeech.labelId())
    return gender?.let { "$name · ${stringResource(it.labelId())}" } ?: name
}

@Composable
internal fun CasesRuleButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = stringResource(R.string.declension_rules))
    }
}
