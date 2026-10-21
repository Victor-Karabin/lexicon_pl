package com.lexicon.application.training

import com.lexicon.boundary.VocabularyRepository
import com.lexicon.model.vocabulary.Word

suspend fun VocabularyRepository.wordsToStudy(vocabularyIds: List<Long>): List<Word> =
    getItemsByIds(vocabularyIds.ifEmpty { studySetWordIds() })
