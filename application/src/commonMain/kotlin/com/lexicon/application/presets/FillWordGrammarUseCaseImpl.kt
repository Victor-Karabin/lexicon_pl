package com.lexicon.application.presets

import co.touchlab.kermit.Logger
import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.VerbConjugationBoundary
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.boundary.WordGrammarGenerator
import com.lexicon.common.runSuspendCatching
import com.lexicon.interactors.presets.FillWordGrammarUseCase
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.VocabularyId

private val log = Logger.withTag("FillWordGrammar")

class FillWordGrammarUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val conjugations: ConjugationRepository,
    private val generator: WordGrammarGenerator,
) : FillWordGrammarUseCase {
    override suspend fun invoke(id: VocabularyId) {
        val word = vocabularyRepository.getWord(id.value) ?: return
        val generated = runSuspendCatching { generator.generate(word.text, word.translation) }
            .onFailure { log.w(it) { "The grammar of a word could not be generated" } }
            .getOrNull() ?: return

        vocabularyRepository.setGrammar(id = id.value, partOfSpeech = generated.partOfSpeech, forms = generated.forms)

        if (generated.partOfSpeech == PartOfSpeech.VERB && generated.conjugation.isNotEmpty()) {
            conjugations.saveUserVerb(
                VerbConjugationBoundary(
                    infinitive = word.text,
                    forms = generated.conjugation,
                    translation = word.translation,
                    example = word.example,
                    aspect = generated.aspect,
                ),
            )
        }
    }
}
