package com.lexicon.presentation.common

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexicon.common.DispatcherProvider
import com.lexicon.common.runSuspendCatching
import com.lexicon.interactors.presets.GetWordGrammarUseCase
import com.lexicon.interactors.presets.WordGrammar
import com.lexicon.model.vocabulary.CaseForms
import com.lexicon.model.vocabulary.Gender
import com.lexicon.model.vocabulary.GrammaticalCase
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.presentation.R
import com.lexicon.presentation.theme.Dimens
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

private const val TAG = "WordGrammar"

private val LabelWidth = 96.dp

@Composable
fun rememberWordGrammar(
    id: VocabularyId?,
    getWordGrammar: GetWordGrammarUseCase = koinInject(),
    dispatchers: DispatcherProvider = koinInject(),
): WordGrammar? {
    var grammar by remember(id) { mutableStateOf<WordGrammar?>(null) }

    LaunchedEffect(id) {
        if (id == null) return@LaunchedEffect
        grammar = runSuspendCatching { withContext(dispatchers.io) { getWordGrammar(id) } }
            .onFailure { failure -> Log.w(TAG, "The grammar of a word could not be read", failure) }
            .getOrNull()
    }

    return grammar
}

@Composable
fun WordGrammarSection(
    grammar: WordGrammar,
    color: Color,
    mutedColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingTiny),
    ) {
        Text(
            text = partOfSpeechLabel(grammar),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = mutedColor,
        )

        grammarRows(grammar).forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                    modifier = Modifier.width(LabelWidth),
                )
                Text(text = value, style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
    }
}

@Composable
private fun partOfSpeechLabel(grammar: WordGrammar): String {
    val name = stringResource(grammar.partOfSpeech.labelId())
    return if (grammar is WordGrammar.Noun) "$name · ${stringResource(grammar.gender.labelId())}" else name
}

@Composable
private fun grammarRows(grammar: WordGrammar): List<Pair<String, String>> =
    when (grammar) {
        is WordGrammar.Noun ->
            GrammaticalCase.entries.mapNotNull { case ->
                grammar.declension[case]?.let { stringResource(case.labelId()) to it.joined() }
            }

        is WordGrammar.Adjective ->
            listOf(
                stringResource(R.string.grammar_masculine) to grammar.forms.masculine,
                stringResource(R.string.grammar_feminine) to grammar.forms.feminine,
                stringResource(R.string.grammar_neuter) to grammar.forms.neuter,
                stringResource(R.string.grammar_plural_personal) to grammar.forms.pluralPersonal,
                stringResource(R.string.grammar_plural_other) to grammar.forms.pluralOther,
            )

        is WordGrammar.Verb ->
            grammar.conjugation
                ?.let { conjugation ->
                    conjugation.persons.map { person -> person.label to conjugation.formsFor(person).joinToString(" / ") }
                }.orEmpty()

        is WordGrammar.Plain -> emptyList()
    }

private fun CaseForms.joined(): String = listOfNotNull(singular, plural).joinToString(" / ")

private fun GrammaticalCase.labelId(): Int =
    when (this) {
        GrammaticalCase.NOMINATIVE -> R.string.grammar_case_nominative
        GrammaticalCase.GENITIVE -> R.string.grammar_case_genitive
        GrammaticalCase.DATIVE -> R.string.grammar_case_dative
        GrammaticalCase.ACCUSATIVE -> R.string.grammar_case_accusative
        GrammaticalCase.INSTRUMENTAL -> R.string.grammar_case_instrumental
        GrammaticalCase.LOCATIVE -> R.string.grammar_case_locative
        GrammaticalCase.VOCATIVE -> R.string.grammar_case_vocative
    }

private fun PartOfSpeech.labelId(): Int =
    when (this) {
        PartOfSpeech.NOUN -> R.string.grammar_pos_n
        PartOfSpeech.VERB -> R.string.grammar_pos_v
        PartOfSpeech.ADJECTIVE -> R.string.grammar_pos_adj
        PartOfSpeech.ADVERB -> R.string.grammar_pos_adv
        PartOfSpeech.PREPOSITION -> R.string.grammar_pos_prep
        PartOfSpeech.CONJUNCTION -> R.string.grammar_pos_conj
        PartOfSpeech.PRONOUN -> R.string.grammar_pos_prn
        PartOfSpeech.NUMERAL -> R.string.grammar_pos_num
        PartOfSpeech.PARTICLE -> R.string.grammar_pos_part
        PartOfSpeech.INTERJECTION -> R.string.grammar_pos_interj
        PartOfSpeech.PHRASE -> R.string.grammar_pos_expr
    }

private fun Gender.labelId(): Int =
    when (this) {
        Gender.MASCULINE_PERSONAL -> R.string.grammar_gender_masculine_personal
        Gender.MASCULINE_ANIMATE -> R.string.grammar_gender_masculine_animate
        Gender.MASCULINE_INANIMATE -> R.string.grammar_gender_masculine_inanimate
        Gender.FEMININE -> R.string.grammar_gender_feminine
        Gender.NEUTER -> R.string.grammar_gender_neuter
        Gender.PLURAL_ONLY -> R.string.grammar_gender_plural_only
    }
