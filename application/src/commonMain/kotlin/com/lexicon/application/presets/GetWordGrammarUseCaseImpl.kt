package com.lexicon.application.presets

import com.lexicon.application.conjugation.conjugationOf
import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.interactors.presets.GetWordGrammarUseCase
import com.lexicon.interactors.presets.WordGrammar
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.VocabularyId
import com.lexicon.model.vocabulary.WordForms

class GetWordGrammarUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val conjugations: ConjugationRepository,
) : GetWordGrammarUseCase {
    override suspend fun invoke(id: VocabularyId): WordGrammar? {
        val word = vocabularyRepository.getWord(id.value) ?: return null
        val partOfSpeech = word.partOfSpeech ?: return null

        return when (val forms = word.forms) {
            is WordForms.Noun -> WordGrammar.Noun(forms.gender, forms.plural)
            is WordForms.Adjective -> WordGrammar.Adjective(forms)
            null ->
                if (partOfSpeech == PartOfSpeech.VERB) {
                    WordGrammar.Verb(conjugations.conjugationOf(word.text))
                } else {
                    WordGrammar.Plain(partOfSpeech)
                }
        }
    }
}
