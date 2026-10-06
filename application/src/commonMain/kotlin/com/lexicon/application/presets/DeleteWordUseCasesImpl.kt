package com.lexicon.application.presets

import com.lexicon.boundary.ConjugationRepository
import com.lexicon.boundary.VocabularyRepository
import com.lexicon.common.runSuspendCatching
import com.lexicon.interactors.presets.DeleteWordUseCase
import com.lexicon.interactors.presets.FillWordGrammarUseCase
import com.lexicon.interactors.presets.RestoreWordUseCase
import com.lexicon.model.vocabulary.PartOfSpeech
import com.lexicon.model.vocabulary.VocabularyId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DeleteWordUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val conjugations: ConjugationRepository,
) : DeleteWordUseCase {
    override suspend fun invoke(id: VocabularyId) {
        val word = vocabularyRepository.getWord(id.value)
        vocabularyRepository.deleteWord(id.value)
        word?.let { conjugations.deleteUserVerb(it.text) }
    }
}

class RestoreWordUseCaseImpl(
    private val vocabularyRepository: VocabularyRepository,
    private val conjugations: ConjugationRepository,
    private val fillWordGrammar: FillWordGrammarUseCase,
    private val appScope: CoroutineScope,
) : RestoreWordUseCase {
    override suspend fun invoke(id: VocabularyId) {
        vocabularyRepository.restoreWord(id.value)
        val word = vocabularyRepository.getWord(id.value) ?: return
        if (word.partOfSpeech == PartOfSpeech.VERB && conjugations.verb(word.text) == null) {
            appScope.launch { runSuspendCatching { fillWordGrammar(id) } }
        }
    }
}
