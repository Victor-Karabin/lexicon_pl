package com.lexicon.interactors.presets

import com.lexicon.interactors.conjugation.VerbConjugation
import com.lexicon.model.vocabulary.Gender
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.WordForms

sealed interface WordGrammar {
    val partOfSpeech: PartOfSpeech

    data class Plain(
        override val partOfSpeech: PartOfSpeech,
    ) : WordGrammar

    data class Noun(
        val gender: Gender,
        val plural: String?,
    ) : WordGrammar {
        override val partOfSpeech: PartOfSpeech get() = PartOfSpeech.NOUN
    }

    data class Adjective(
        val forms: WordForms.Adjective,
    ) : WordGrammar {
        override val partOfSpeech: PartOfSpeech get() = PartOfSpeech.ADJECTIVE
    }

    data class Verb(
        val conjugation: VerbConjugation?,
    ) : WordGrammar {
        override val partOfSpeech: PartOfSpeech get() = PartOfSpeech.VERB
    }
}

fun interface GetWordGrammarUseCase {
    suspend operator fun invoke(id: VocabularyId): WordGrammar?
}
