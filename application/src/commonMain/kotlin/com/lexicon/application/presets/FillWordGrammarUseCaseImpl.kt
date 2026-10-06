package com.lexicon.application.presets

import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.boundary.WordGrammarGenerator
import com.lexicon.interactors.presets.FillWordGrammarUseCase
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.VocabularyId

class FillWordGrammarUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val conjugations: ConjugationRepository,
    private val generator: WordGrammarGenerator,
) : FillWordGrammarUseCase {
    override suspend fun invoke(id: VocabularyId) {
        val word = vocabularyRepository.getWord(id.value) ?: return
        val generated = runCatching { generator.generate(word.text, word.translation) }.getOrNull() ?: return

        vocabularyRepository.setGrammar(id = id.value, partOfSpeech = generated.partOfSpeech, forms = generated.forms)

        if (generated.partOfSpeech == PartOfSpeech.VERB && generated.conjugation.isNotEmpty()) {
            conjugations.saveUserVerb(
                VerbConjugationBoundary(
                    infinitive = word.text,
                    forms = generated.conjugation,
                    translation = word.translation,
                    example = word.example,
                ),
            )
        }
    }
}
