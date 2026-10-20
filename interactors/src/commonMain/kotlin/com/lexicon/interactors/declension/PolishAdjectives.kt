package com.lexicon.interactors.declension

import com.lexicon.model.vocabulary.GrammaticalCase
import com.lexicon.model.vocabulary.WordForms
import kotlinx.collections.immutable.ImmutableList

object PolishAdjectives {
    fun decline(forms: WordForms.Adjective): Map<DeclensionColumn, Map<GrammaticalCase, ImmutableList<String>>>? {
        val masculine = forms.masculine.trim()
        val feminine = forms.feminine.trim()
        val neuter = forms.neuter.trim()
        val regular = masculine.none(Char::isWhitespace) &&
            (masculine.endsWith('y') || masculine.endsWith('i')) &&
            feminine.endsWith('a') &&
            neuter.endsWith('e')
        if (!regular) return null

        val oblique = neuter.dropLast(1)
        val femStem = feminine.dropLast(1)
        val femOblique = femStem + if (femStem.endsWith('k') || femStem.endsWith('g')) "iej" else "ej"

        val genitive = oblique + "ego"
        val dative = oblique + "emu"
        val withM = masculine + "m"
        val withCh = masculine + "ch"
        val withMi = masculine + "mi"
        val femAccusative = femStem + "ą"
        val personal = forms.pluralPersonal.trim()
        val other = forms.pluralOther.trim()

        return mapOf(
            DeclensionColumn.MASCULINE to table(masculine, genitive, dative, listOf(genitive, masculine), withM, withM, masculine),
            DeclensionColumn.FEMININE to
                table(feminine, femOblique, femOblique, listOf(femAccusative), femAccusative, femOblique, feminine),
            DeclensionColumn.NEUTER to table(neuter, genitive, dative, listOf(neuter), withM, withM, neuter),
            DeclensionColumn.PLURAL_PERSONAL to table(personal, withCh, withM, listOf(withCh), withMi, withCh, personal),
            DeclensionColumn.PLURAL_OTHER to table(other, withCh, withM, listOf(other), withMi, withCh, other),
        )
    }

    private fun table(
        nominative: String,
        genitive: String,
        dative: String,
        accusative: List<String>,
        instrumental: String,
        locative: String,
        vocative: String,
    ): Map<GrammaticalCase, ImmutableList<String>> =
        mapOf(
            GrammaticalCase.NOMINATIVE to listOf(nominative).toForms(),
            GrammaticalCase.GENITIVE to listOf(genitive).toForms(),
            GrammaticalCase.DATIVE to listOf(dative).toForms(),
            GrammaticalCase.ACCUSATIVE to accusative.toForms(),
            GrammaticalCase.INSTRUMENTAL to listOf(instrumental).toForms(),
            GrammaticalCase.LOCATIVE to listOf(locative).toForms(),
            GrammaticalCase.VOCATIVE to listOf(vocative).toForms(),
        ).filterValues { it.isNotEmpty() }
}
